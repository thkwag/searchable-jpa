package com.github.thkwag.searchable.core.service.specification;

import com.github.thkwag.searchable.core.condition.SearchCondition;
import com.github.thkwag.searchable.core.condition.SearchCondition.Node;
import com.github.thkwag.searchable.core.condition.operator.LogicalOperator;
import com.github.thkwag.searchable.core.service.join.JoinManager;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds JPA Specification from SearchCondition.
 * Thread-safe and immutable implementation.
 *
 * @param <T> The entity type
 * @param <P>
 */
public class SearchableSpecificationBuilder<T> {
    private static final Logger log = LoggerFactory.getLogger(SearchableSpecificationBuilder.class);
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final SearchCondition<?> condition;
    private final EntityManager entityManager;
    private final Class<T> entityClass;

    public SearchableSpecificationBuilder(@NonNull SearchCondition<?> condition,
                                          @NonNull EntityManager entityManager,
                                          @NonNull Class<T> entityClass) {
        this.condition = condition;
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    public static <T> SearchableSpecificationBuilder<T> of(
            @NonNull SearchCondition<?> condition,
            @NonNull EntityManager entityManager,
            @NonNull Class<T> entityClass) {
        return new SearchableSpecificationBuilder<>(condition, entityManager, entityClass);
    }

    /**
     * Creates Sort object from SearchCondition orders.
     * Returns Sort.unsorted() if no orders defined.
     */
    private Sort createSort() {
        SearchCondition.Sort sortCondition = condition.getSort();
        if (sortCondition == null) {
            return Sort.unsorted();
        }

        List<SearchCondition.Order> orders = sortCondition.getOrders();
        return orders.isEmpty()
                ? Sort.unsorted()
                : Sort.by(orders.stream()
                .map(this::createOrder)
                .collect(Collectors.toList()));
    }

    /**
     * Creates Sort.Order from SearchCondition.Order.
     * Direction defaults to ASC if not specified.
     */
    private Sort.Order createOrder(SearchCondition.Order order) {
        String field = order.getEntityField();
        if (field == null || field.isEmpty()) {
            field = order.getField();
        }
        Sort.Direction direction = order.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        return new Sort.Order(direction, field);
    }

    /**
     * Creates and combines predicates from condition nodes.
     * Returns null if no predicates created.
     */
    private Predicate createPredicates(Root<T> root,
                                       javax.persistence.criteria.CriteriaQuery<?> query,
                                       CriteriaBuilder cb,
                                       SpecificationBuilder<T> specBuilder) {
        List<Node> nodes = condition.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        // Always apply distinct for safety
        query.distinct(true);

        // Process the first node
        Node firstNode = nodes.get(0);
        Predicate result = specBuilder.build(root, query, cb, firstNode);
        if (result == null) {
            return null;
        }

        // Process remaining nodes in order, combining them with their respective operators
        for (int i = 1; i < nodes.size(); i++) {
            Node currentNode = nodes.get(i);
            Predicate currentPredicate = specBuilder.build(root, query, cb, currentNode);
            if (currentPredicate == null) continue;

            if (currentNode.getOperator() == LogicalOperator.OR) {
                result = cb.or(result, currentPredicate);
            } else {
                result = cb.and(result, currentPredicate);
            }
        }

        return result;
    }

    /**
     * Builds SpecificationWithPageable from SearchCondition.
     * Thread-safe method that creates new instance each time.
     *
     * @return SpecificationWithPageable containing specification and page request
     */
    public SpecificationWithPageable<T> build() {
        return new SpecificationWithPageable<>(
                buildSpecification(),
                buildPageRequest()
        );
    }

    private Specification<T> buildSpecification() {
        return (root, query, cb) -> {
            // Apply fetch joins only for non-count queries
            if (!query.getResultType().equals(Long.class)) {
                Set<Class<?>> processedEntityTypes = new HashSet<>();
                processedEntityTypes.add(entityClass);
                applyFetchJoins(root, processedEntityTypes);
                query.distinct(true);
            }

            JoinManager<T> joinManager = new JoinManager<>(entityManager, entityClass, root);
            PredicateBuilder<T> predicateBuilder = new PredicateBuilder<>(cb, joinManager);
            SpecificationBuilder<T> specBuilder = new SpecificationBuilder<>(predicateBuilder);
            return createPredicates(root, query, cb, specBuilder);
        };
    }

    private void applyFetchJoins(Root<T> root, Set<Class<?>> processedEntityTypes) {
        ManagedType<?> managedType = entityManager.getMetamodel().managedType(root.getJavaType());

        // Process ToOne relationships first
        for (Attribute<?, ?> attribute : managedType.getAttributes()) {
            if (attribute.isAssociation() && !attribute.isCollection()) {
                Class<?> targetType = attribute.getJavaType();
                if (!processedEntityTypes.contains(targetType)) {
                    root.fetch(attribute.getName(), JoinType.LEFT);
                    processedEntityTypes.add(targetType);
                }
            }
        }

        // Process ToMany relationships and their ToOne relationships
        for (Attribute<?, ?> attribute : managedType.getAttributes()) {
            if (attribute.isAssociation() && attribute.isCollection()) {
                PluralAttribute<?, ?, ?> pluralAttribute = (PluralAttribute<?, ?, ?>) attribute;
                Class<?> elementType = pluralAttribute.getElementType().getJavaType();

                Fetch<?, ?> collectionFetch = root.fetch(attribute.getName(), JoinType.LEFT);

                // Process ToOne relationships of collection entities
                ManagedType<?> elementManagedType = entityManager.getMetamodel().managedType(elementType);
                for (Attribute<?, ?> elementAttribute : elementManagedType.getAttributes()) {
                    if (elementAttribute.isAssociation() && !elementAttribute.isCollection()) {
                        collectionFetch.fetch(elementAttribute.getName(), JoinType.LEFT);
                    }
                }
            }
        }
    }

    private PageRequest buildPageRequest() {
        Integer pageNum = condition.getPage();
        Integer sizeNum = condition.getSize();

        int page = pageNum != null ? Math.max(0, pageNum) : 0;
        int size = sizeNum != null ? (sizeNum > 0 ? sizeNum : DEFAULT_PAGE_SIZE) : DEFAULT_PAGE_SIZE;

        // Skip pagination if both page and size are 0 (fetch all data)
        if (page == 0 && size == 0) {
            return PageRequest.of(page, size, createSort());
        }

        return PageRequest.of(page, size, createSort());
    }
}

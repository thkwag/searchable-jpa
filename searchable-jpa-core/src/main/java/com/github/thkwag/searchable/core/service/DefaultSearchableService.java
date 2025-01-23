package com.github.thkwag.searchable.core.service;

import com.github.thkwag.searchable.core.condition.SearchCondition;
import com.github.thkwag.searchable.core.exception.SearchableConfigurationException;
import com.github.thkwag.searchable.core.service.specification.SearchableSpecificationBuilder;
import com.github.thkwag.searchable.core.service.specification.SpecificationWithPageable;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.NonNull;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public class DefaultSearchableService<T, ID> implements SearchableService<T> {

    protected final JpaRepository<T, ID> repository;
    private final JpaSpecificationExecutor<T> specificationExecutor;
    private final Class<T> entityClass;
    private final EntityManager entityManager;
    private final ProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

    @SuppressWarnings("unchecked")
    public DefaultSearchableService(JpaRepository<T, ID> repository, EntityManager entityManager) {
        if (!(repository instanceof JpaSpecificationExecutor<?>)) {
            throw new SearchableConfigurationException("Repository must implement JpaSpecificationExecutor");
        }
        this.repository = repository;
        this.specificationExecutor = (JpaSpecificationExecutor<T>) repository;
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
        this.entityManager = entityManager;
    }

    protected SpecificationWithPageable<T> createSpecification(SearchCondition<?> searchCondition) {
        return SearchableSpecificationBuilder.of(searchCondition, entityManager, entityClass).build();
    }

    @Override
    @NonNull
    public Page<T> findAllWithSearch(@NonNull SearchCondition<?> searchCondition) {
        SpecificationWithPageable<T> spec = createSpecification(searchCondition);
        return specificationExecutor.findAll(spec.getSpecification(), spec.getPageRequest());
    }

    @Override
    @NonNull
    public <P> Page<P> findAllWithSearch(@NonNull SearchCondition<?> searchCondition, Class<P> projectionClass) {
        SpecificationWithPageable<T> spec = createSpecification(searchCondition);

        if (!projectionClass.isInterface()) {
            throw new SearchableConfigurationException("Projection class must be an interface");
        }

        return specificationExecutor.findAll(spec.getSpecification(), spec.getPageRequest())
                .map(entity -> projectionFactory.createProjection(projectionClass, entity));
    }

    @Override
    @NonNull
    public Optional<T> findOneWithSearch(@NonNull SearchCondition<?> searchCondition) {
        SpecificationWithPageable<T> spec = createSpecification(searchCondition);
        return specificationExecutor.findOne(spec.getSpecification());
    }

    @Override
    @NonNull
    public Optional<T> findFirstWithSearch(@NonNull SearchCondition<?> searchCondition) {
        SpecificationWithPageable<T> spec = createSpecification(searchCondition);
        return specificationExecutor.findAll(spec.getSpecification(), spec.getPageRequest().withPage(0).first())
                .stream()
                .findFirst();
    }

    @Override
    public long deleteWithSearch(@NonNull SearchCondition<?> searchCondition) {
        SpecificationWithPageable<T> spec = createSpecification(searchCondition);
        List<T> toDelete = specificationExecutor.findAll(spec.getSpecification());
        repository.deleteAll(toDelete);
        return toDelete.size();
    }

    @Override
    public long countWithSearch(@NonNull SearchCondition<?> searchCondition) {
        SpecificationWithPageable<T> spec = createSpecification(searchCondition);
        return specificationExecutor.count(spec.getSpecification());
    }

    @Override
    public boolean existsWithSearch(@NonNull SearchCondition<?> searchCondition) {
        SpecificationWithPageable<T> spec = createSpecification(searchCondition);
        return specificationExecutor.exists(spec.getSpecification());
    }

    @SuppressWarnings("unchecked")
    @Override
    public long updateWithSearch(@NonNull SearchCondition<?> searchCondition, @NonNull Object updateData) {

        if (updateData.getClass() != entityClass) {
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration()
                    .setSkipNullEnabled(true)
                    .setFieldMatchingEnabled(true)
                    .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                    .setPropertyCondition(ctx -> ctx.getSource() != null)
                    .setMatchingStrategy(org.modelmapper.convention.MatchingStrategies.STRICT);
            updateData = modelMapper.map(updateData, entityClass);
        } else {
            updateData = (T) updateData;
        }

        try {
            // Find entities to update using specification
            SpecificationWithPageable<T> specification = createSpecification(searchCondition);
            List<T> entitiesToUpdate = specificationExecutor.findAll(specification.getSpecification());

            // Apply updates one by one
            for (T entity : entitiesToUpdate) {
                // Get the ID of the entity
                ID id = (ID) entityClass.getMethod("getId").invoke(entity);

                // Reload the entity from the persistence context
                T managedEntity = repository.findById(id)
                        .orElseThrow(() -> new SearchableConfigurationException("Entity not found with id: " + id));

                // Copy non-null properties from updateData
                copyNonNullProperties(updateData, managedEntity);

                // Save the updated entity
                repository.save(managedEntity);
            }

            return entitiesToUpdate.size();
        } catch (Exception e) {
            throw new SearchableConfigurationException("Failed to perform batch update", e);
        }
    }

    /**
     * Copies non-null properties from the source object to the target object.
     *
     * @param source the source object
     * @param target the target object
     * @throws IllegalAccessException if field access is denied
     */
    private void copyNonNullProperties(Object source, T target) throws IllegalAccessException {
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(source);
            if (value != null && !Collection.class.isAssignableFrom(field.getType())) {
                field.set(target, value);
            }
        }
    }
}

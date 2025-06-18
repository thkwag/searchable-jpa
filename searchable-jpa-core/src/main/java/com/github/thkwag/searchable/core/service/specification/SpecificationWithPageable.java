package com.github.thkwag.searchable.core.service.specification;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * Holds a JPA Specification and PageRequest together.
 * 
 * @deprecated This class uses OFFSET-based pagination which has performance issues with large datasets.
 * Use SearchableSpecificationBuilder.buildAndExecuteWithCursor() instead for cursor-based pagination.
 */
@Data
@Deprecated
public class SpecificationWithPageable<T> {
    private final Specification<T> specification;
    private final PageRequest pageRequest;

    SpecificationWithPageable(Specification<T> specification, PageRequest pageRequest) {
        this.specification = specification;
        this.pageRequest = pageRequest;
    }
}
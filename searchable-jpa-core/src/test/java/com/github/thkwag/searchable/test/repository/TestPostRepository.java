package com.github.thkwag.searchable.test.repository;

import com.github.thkwag.searchable.test.entity.TestPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestPostRepository extends JpaRepository<TestPost, Long>, JpaSpecificationExecutor<TestPost> {
} 
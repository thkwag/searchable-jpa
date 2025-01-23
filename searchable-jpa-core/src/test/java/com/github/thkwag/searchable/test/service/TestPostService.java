package com.github.thkwag.searchable.test.service;

import com.github.thkwag.searchable.core.service.DefaultSearchableService;
import com.github.thkwag.searchable.test.entity.TestPost;
import com.github.thkwag.searchable.test.repository.TestPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@Service
@Transactional
public class TestPostService extends DefaultSearchableService<TestPost, Long> {
    public TestPostService(TestPostRepository repository, EntityManager entityManager) {
        super(repository, entityManager);
    }

} 
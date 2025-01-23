package com.github.thkwag.searchable.example.service;

import com.github.thkwag.searchable.core.service.DefaultSearchableService;
import com.github.thkwag.searchable.example.entity.User;
import com.github.thkwag.searchable.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@Service
@Transactional
public class UserService extends DefaultSearchableService<User, Long> {
    public UserService(UserRepository repository, EntityManager entityManager) {
        super(repository, entityManager);
    }
} 
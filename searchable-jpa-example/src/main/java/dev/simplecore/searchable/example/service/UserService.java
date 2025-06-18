package dev.simplecore.searchable.example.service;

import dev.simplecore.searchable.core.service.DefaultSearchableService;
import dev.simplecore.searchable.example.entity.User;
import dev.simplecore.searchable.example.repository.UserRepository;
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
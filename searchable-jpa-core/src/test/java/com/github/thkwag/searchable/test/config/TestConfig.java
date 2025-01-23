package com.github.thkwag.searchable.test.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Configuration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.github.thkwag.searchable.test.entity")
@EnableJpaRepositories(basePackages = "com.github.thkwag.searchable.test.repository")
@ComponentScan(basePackages = "com.github.thkwag.searchable.test")
public class TestConfig {
    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public EntityManager entityManager() {
        return entityManager;
    }
} 
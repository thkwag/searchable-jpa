package com.github.thkwag.searchable.example.config;

import com.github.javafaker.Faker;
import com.github.thkwag.searchable.example.entity.User;
import com.github.thkwag.searchable.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            Faker faker = new Faker(new Locale("en"));
            List<User> users = new ArrayList<>();

            // for (int i = 0; i < 50; i++) {
            //     User user = new User();
            //     user.setName(faker.name().fullName());
            //     user.setEmail(faker.internet().emailAddress());
            //     user.setAge(faker.number().numberBetween(18, 80));
            //     users.add(user);
            // }

            userRepository.saveAll(users);
        };
    }
} 
package com.github.thkwag.searchable.example.service;

import com.github.javafaker.Faker;
import com.github.thkwag.searchable.example.entity.Author;
import com.github.thkwag.searchable.example.entity.Comment;
import com.github.thkwag.searchable.example.entity.Post;
import com.github.thkwag.searchable.example.enums.PostStatus;
import com.github.thkwag.searchable.example.repository.AuthorRepository;
import com.github.thkwag.searchable.example.repository.CommentRepository;
import com.github.thkwag.searchable.example.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleDataLoaderService {

    private final AuthorRepository authorRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final Faker faker = new Faker(new Locale("en"));

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadSampleData() {
        if (postRepository.count() > 0) {
            log.info("Sample data already exists. Skipping data generation.");
            return;
        }

        log.info("Starting to generate sample data...");
        List<Author> authors = createAuthors(50);
        List<Post> posts = createPosts(authors, 500);
        createComments(authors, posts, 1000);
        log.info("Finished generating sample data");
    }

    private List<Author> createAuthors(int count) {
        List<Author> authors = new ArrayList<>();
        Set<String> usedEmails = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String email;
            do {
                String firstName = faker.name().firstName();
                String lastName = faker.name().lastName();
                email = String.format("%s.%s@%s",
                        firstName.toLowerCase(),
                        lastName.toLowerCase(),
                        faker.internet().domainName());
            } while (usedEmails.contains(email));

            usedEmails.add(email);

            Author author = new Author();
            author.setName(faker.name().fullName());
            author.setEmail(email);
            authors.add(authorRepository.save(author));
        }

        log.info("Created {} authors", authors.size());
        return authors;
    }

    private List<Post> createPosts(List<Author> authors, int count) {
        List<Post> posts = new ArrayList<>();
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(6);

        for (int i = 0; i < count; i++) {
            Author author = authors.get(faker.random().nextInt(authors.size()));
            Post post = createRandomPost(author, startDate, endDate);
            posts.add(post);
        }

        return postRepository.saveAll(posts);
    }

    private Post createRandomPost(Author author, LocalDateTime startDate, LocalDateTime endDate) {
        Post post = new Post();

        String topic = faker.programmingLanguage().name();
        String concept = faker.company().buzzword();
        String action = faker.company().bs();
        post.setTitle(String.format("How to %s %s with %s", action, concept, topic));

        StringBuilder content = new StringBuilder();
        content.append("# Introduction\n\n")
                .append(faker.lorem().paragraph(5))
                .append("\n\n")
                .append("## Key Points\n\n");

        int points = faker.random().nextInt(3, 6);
        for (int i = 0; i < points; i++) {
            content.append(String.format("%d. %s\n", i + 1, faker.company().bs()));
        }

        content.append("\n\n## Implementation\n\n")
                .append(faker.lorem().paragraph(3))
                .append("\n\n")
                .append("### Code Example\n\n")
                .append("```java\n")
                .append(faker.lorem().paragraph(1))
                .append("\n```\n\n")
                .append("## Conclusion\n\n")
                .append(faker.lorem().paragraph(2));

        post.setContent(content.toString());
        post.setAuthor(author);
        post.setStatus(faker.random().nextDouble() < 0.8 ? PostStatus.PUBLISHED : PostStatus.DRAFT);
        post.setViewCount((long) faker.random().nextInt(0, 100000));

        Date randomDate = faker.date().between(
                Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant())
        );
        post.setCreatedAt(LocalDateTime.ofInstant(randomDate.toInstant(), ZoneId.systemDefault()));

        return post;
    }

    private void createComments(List<Author> authors, List<Post> posts, int count) {
        List<Comment> comments = new ArrayList<>();
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(6);

        for (int i = 0; i < count; i++) {
            Comment comment = new Comment();
            comment.setAuthor(authors.get(faker.random().nextInt(authors.size())));
            comment.setPost(posts.get(faker.random().nextInt(posts.size())));
            comment.setContent(faker.lorem().paragraph(1));

            Date randomDate = faker.date().between(
                    Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant()),
                    Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant())
            );
            comment.setCreatedAt(LocalDateTime.ofInstant(randomDate.toInstant(), ZoneId.systemDefault()));

            comments.add(comment);
        }

        commentRepository.saveAll(comments);
        log.info("Created {} comments", comments.size());
    }
}
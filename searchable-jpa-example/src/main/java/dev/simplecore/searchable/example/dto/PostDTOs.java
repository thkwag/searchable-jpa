package dev.simplecore.searchable.example.dto;

import dev.simplecore.searchable.core.annotation.SearchableField;
import dev.simplecore.searchable.example.entity.Author;
import dev.simplecore.searchable.example.enums.PostStatus;
import static dev.simplecore.searchable.core.condition.operator.SearchOperator.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class PostDTOs {

    @Getter
    @Setter
    public static class PostSearchDTO {

        @Schema(
                description = "Post id",
                example = "1"
        )
        @SearchableField(entityField = "id", operators = {EQUALS})
        private Long id;

        @Schema(
                description = "Post title to search",
                example = "Welcome to my blog"
        )
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        @SearchableField(entityField = "title", operators = {EQUALS, CONTAINS, STARTS_WITH}, sortable = true)
        private String searchTitle;

        @Schema(
                description = "Post status",
                example = "PUBLISHED"
        )
        @SearchableField(operators = {EQUALS, NOT_EQUALS, IN, NOT_IN, IS_NULL, IS_NOT_NULL})
        private PostStatus status;

        @Schema(
                description = "Number of views",
                example = "100"
        )
        @SearchableField(operators = {GREATER_THAN, LESS_THAN, BETWEEN}, sortable = true)
        private Long viewCount;

        @Schema(
                description = "Post creation date and time",
                example = "2024-01-01T00:00:00",
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        @SearchableField(operators = {GREATER_THAN, LESS_THAN, BETWEEN}, sortable = true)
        private LocalDateTime createdAt;

        @Schema(
                description = "Post last update date and time",
                example = "2024-01-01T00:00:00",
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        @SearchableField(operators = {GREATER_THAN, LESS_THAN, BETWEEN}, sortable = true)
        private LocalDateTime updatedAt;

        @Schema(
                description = "Author's name",
                example = "John Doe"
        )
        @Size(max = 50, message = "Author name cannot exceed 50 characters")
        @SearchableField(entityField = "author.name", operators = {EQUALS, CONTAINS, STARTS_WITH})
        private String authorName;

        @Schema(
                description = "Author's email address",
                example = "john.doe@example.com"
        )
        @Size(max = 100, message = "Email address cannot exceed 100 characters")
        @SearchableField(entityField = "author.email", operators = {EQUALS, CONTAINS, ENDS_WITH})
        private String authorEmail;

    }

    @Data
    public static class PostUpdateDTO {
        private String title;
        private String content;

        @Schema(description = "Post status", example = "PUBLISHED")
        private PostStatus status;
    }


    public interface PostListProjection {
        Long getId();

        String getTitle();

        PostStatus getStatus();

        Long getViewCount();

        LocalDateTime getCreatedAt();

        LocalDateTime getUpdatedAt();

        Author getAuthor();
    }

}
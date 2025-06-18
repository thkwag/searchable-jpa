package dev.simplecore.searchable.example.dto;

import dev.simplecore.searchable.core.annotation.SearchableField;
import static dev.simplecore.searchable.core.condition.operator.SearchOperator.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "User DTO for testing searchable annotations")
public class UserDTO {

    @Schema(description = "User ID", example = "1")
    @SearchableField(operators = {EQUALS})
    private Long id;

    @Schema(description = "User name", example = "John Doe")
    @SearchableField(operators = {EQUALS, CONTAINS, STARTS_WITH}, sortable = true)
    private String name;

    @Schema(description = "User email", example = "john.doe@example.com")
    @SearchableField(operators = {EQUALS, CONTAINS, ENDS_WITH})
    private String email;

    @Schema(description = "User age", example = "30")
    @SearchableField(operators = {EQUALS, GREATER_THAN, LESS_THAN}, sortable = true)
    private Integer age;

    @Schema(description = "User registration date", example = "2024-01-01T00:00:00")
    @SearchableField(operators = {EQUALS, GREATER_THAN, LESS_THAN}, sortable = true)
    private LocalDateTime registeredAt;
} 
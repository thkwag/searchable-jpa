package com.github.thkwag.searchable.example.dto;

import com.github.thkwag.searchable.core.annotation.SearchableField;
import com.github.thkwag.searchable.core.condition.operator.SearchOperator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "User DTO for testing searchable annotations")
public class UserDTO {

    @Schema(description = "User ID", example = "1")
    @SearchableField(operators = {SearchOperator.EQUALS})
    private Long id;

    @Schema(description = "User name", example = "John Doe")
    @SearchableField(operators = {SearchOperator.EQUALS, SearchOperator.CONTAINS, SearchOperator.STARTS_WITH}, sortable = true)
    private String name;

    @Schema(description = "User email", example = "john.doe@example.com")
    @SearchableField(operators = {SearchOperator.EQUALS, SearchOperator.CONTAINS, SearchOperator.ENDS_WITH})
    private String email;

    @Schema(description = "User age", example = "30")
    @SearchableField(operators = {SearchOperator.EQUALS, SearchOperator.GREATER_THAN, SearchOperator.LESS_THAN}, sortable = true)
    private Integer age;

    @Schema(description = "User registration date", example = "2024-01-01T00:00:00")
    @SearchableField(operators = {SearchOperator.EQUALS, SearchOperator.GREATER_THAN, SearchOperator.LESS_THAN}, sortable = true)
    private LocalDateTime registeredAt;
} 
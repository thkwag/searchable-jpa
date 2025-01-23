package com.github.thkwag.searchable.core.exception;

/**
 * Exception thrown when there is an error during search operations.
 * This includes predicate building errors, specification creation failures, and query execution issues.
 */
@SuppressWarnings({"unused"})
public class SearchableOperationException extends SearchableException {

    public SearchableOperationException(String message) {
        super(message);
    }

    public SearchableOperationException(String message, Throwable cause) {
        super(message, cause);
    }
} 
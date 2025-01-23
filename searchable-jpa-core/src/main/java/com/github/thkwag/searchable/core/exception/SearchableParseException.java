package com.github.thkwag.searchable.core.exception;

/**
 * Exception thrown when parsing search parameters fails.
 */
@SuppressWarnings({"unused"})
public class SearchableParseException extends RuntimeException {
    public SearchableParseException(String message) {
        super(message);
    }

    public SearchableParseException(String message, Throwable cause) {
        super(message, cause);
    }
} 
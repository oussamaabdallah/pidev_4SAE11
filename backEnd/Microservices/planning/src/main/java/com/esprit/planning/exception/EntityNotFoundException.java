package com.esprit.planning.exception;

/**
 * Thrown when an entity (ProgressUpdate, ProgressComment, etc.) is not found.
 * Prefer this over generic RuntimeException for clearer API contracts and SonarQube compliance.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Long id) {
        super(entityName + " not found with id: " + id);
    }
}

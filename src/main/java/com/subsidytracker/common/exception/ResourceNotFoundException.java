package com.subsidytracker.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested entity (Application, Beneficiary, Scheme, etc.)
 * is not found in the database.
 *
 * Spring automatically returns HTTP 404 thanks to @ResponseStatus.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Convenience constructor: "Application not found with id: 42"
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }
}

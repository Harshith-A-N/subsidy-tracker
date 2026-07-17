package com.subsidytracker.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a business rule is violated.
 *
 * Examples:
 * - Trying to verify an application that hasn't been scored yet
 * - A Field Officer trying to perform a District-level verification
 * - Trying to approve an already-rejected application
 *
 * Spring automatically returns HTTP 400 thanks to @ResponseStatus.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}

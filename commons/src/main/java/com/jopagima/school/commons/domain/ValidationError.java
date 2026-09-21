package com.jopagima.school.commons.domain;

/**
 * ValidationError
 */
public class ValidationError extends RuntimeException {

    public static ValidationError create(String message) {
        return new ValidationError(message);
    }

    private ValidationError(String message) {
        super(message);
    }

}

package com.jopagima.school.commons.domain;



public class DomainError extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final ErrorType type;

    private DomainError(ErrorType type, String message) {
        super(message);
        this.type = type;
    }
    public static DomainError createNotFound(String message) {
        return new DomainError(ErrorType.notFound, message);
    }

    public static DomainError createAlreadyExists(String message) {
        return new DomainError(ErrorType.alreadyExists, message);
    }

    public static DomainError create(String message) {
        return new DomainError(ErrorType.other, message);
    }

    public ErrorType getType() {
        return type;
    }

}

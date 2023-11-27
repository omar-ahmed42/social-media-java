package com.omarahmed42.socialmedia.exception;

public class ForbiddenException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Forbidden access to resource";

    public ForbiddenException() {
        super(DEFAULT_MESSAGE);
    }

    public ForbiddenException(String message) {
        super(message);
    }
}

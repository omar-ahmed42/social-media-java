package com.omarahmed42.socialmedia.exception;

public class ForbiddenPostAccessException extends ForbiddenException {
    private static final String DEFAULT_MESSAGE = "Forbidden: Cannot access post";

    public ForbiddenPostAccessException() {
        super(DEFAULT_MESSAGE);
    }

    public ForbiddenPostAccessException(String message) {
        super(message);
    }
}

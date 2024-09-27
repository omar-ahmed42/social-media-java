package com.omarahmed42.socialmedia.exception;

public class UnauthorizedException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Unauthorized";

    public UnauthorizedException() {
        super(DEFAULT_MESSAGE);
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}

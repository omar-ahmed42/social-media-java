package com.omarahmed42.socialmedia.exception;

public class UsernameAlreadyExistsException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "Username already exists";

    public UsernameAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }

    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}

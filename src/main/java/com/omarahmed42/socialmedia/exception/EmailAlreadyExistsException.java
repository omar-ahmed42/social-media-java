package com.omarahmed42.socialmedia.exception;

public class EmailAlreadyExistsException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "Email already exists";

    public EmailAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}

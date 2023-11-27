package com.omarahmed42.socialmedia.exception;

public class UserNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "User not found";

    public UserNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}

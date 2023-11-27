package com.omarahmed42.socialmedia.exception;

public class PostNotFoundException extends NotFoundException {

    private static final String DEFAULT_MESSAGE = "Post not found";

    public PostNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public PostNotFoundException(String message) {
        super(message);
    }
}

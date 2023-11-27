package com.omarahmed42.socialmedia.exception;

public class CommentNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "Comment not found";

    public CommentNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public CommentNotFoundException(String message) {
        super(message);
    }
}

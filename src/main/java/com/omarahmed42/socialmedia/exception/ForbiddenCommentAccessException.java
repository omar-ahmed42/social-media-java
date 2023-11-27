package com.omarahmed42.socialmedia.exception;

public class ForbiddenCommentAccessException extends ForbiddenException {
    private static final String DEFAULT_MESSAGE = "Forbidden: Cannot access comment";

    public ForbiddenCommentAccessException() {
        super(DEFAULT_MESSAGE);
    }

    public ForbiddenCommentAccessException(String message) {
        super(message);
    }
}

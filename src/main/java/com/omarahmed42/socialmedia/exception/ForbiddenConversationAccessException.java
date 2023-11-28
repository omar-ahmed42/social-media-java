package com.omarahmed42.socialmedia.exception;

public class ForbiddenConversationAccessException extends ForbiddenException {
    private static final String DEFAULT_MESSAGE = "Forbidden: Cannot access conversation";

    public ForbiddenConversationAccessException() {
        super(DEFAULT_MESSAGE);
    }

    public ForbiddenConversationAccessException(String message) {
        super(message);
    }
}

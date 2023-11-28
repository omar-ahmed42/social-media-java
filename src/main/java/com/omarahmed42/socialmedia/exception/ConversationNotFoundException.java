package com.omarahmed42.socialmedia.exception;

public class ConversationNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "Conversation not found";

    public ConversationNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public ConversationNotFoundException(String message) {
        super(message);
    }
}

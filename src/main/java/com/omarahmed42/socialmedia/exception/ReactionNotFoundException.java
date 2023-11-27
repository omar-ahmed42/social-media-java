package com.omarahmed42.socialmedia.exception;

public class ReactionNotFoundException extends NotFoundException {

    private static final String DEFAULT_MESSAGE = "Reaction not found";

    public ReactionNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public ReactionNotFoundException(String message) {
        super(message);
    }

}

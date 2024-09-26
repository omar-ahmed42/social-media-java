package com.omarahmed42.socialmedia.exception;

public class TokenDeniedException extends ForbiddenException {

    private static final String DEFAULT_MESSAGE = "Token Denied";

    public TokenDeniedException(String msg) {
        super(msg);
    }

    public TokenDeniedException() {
        super(DEFAULT_MESSAGE);
    }
}

package com.omarahmed42.socialmedia.exception;

public class AccessTokenExpiredException extends AccessDeniedException {
    private static final String DEFAULT_MESSAGE = "Access token expired";

    public AccessTokenExpiredException() {
        super(DEFAULT_MESSAGE);
    }

    public AccessTokenExpiredException(String msg) {
        super(msg);
    }
}
package com.omarahmed42.socialmedia.exception;

public class AccessDeniedException extends UnauthorizedException {
    private static final String DEFAULT_MESSAGE = "Access Denied";

    public AccessDeniedException(String msg) {
        super(msg);
    }

    public AccessDeniedException() {
        super(DEFAULT_MESSAGE);
    }
}

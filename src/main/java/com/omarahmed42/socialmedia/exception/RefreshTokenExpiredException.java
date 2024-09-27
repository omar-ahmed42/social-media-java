package com.omarahmed42.socialmedia.exception;

public class RefreshTokenExpiredException extends AccessDeniedException {
    public RefreshTokenExpiredException(String msg) {
        super(msg);
    }
}

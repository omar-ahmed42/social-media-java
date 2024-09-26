package com.omarahmed42.socialmedia.exception;

public class RefreshTokenExpiredException extends RuntimeException {
    public RefreshTokenExpiredException(String msg) {
        super(msg);
    }
}

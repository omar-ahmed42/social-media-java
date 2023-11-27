package com.omarahmed42.socialmedia.exception;

public class UnderAgeException extends BadRequestException {
    public UnderAgeException(String message) {
        super(message);
    }
}

package com.omarahmed42.socialmedia.exception;

public class InternalServerErrorException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Internal server error while processing request";

    public InternalServerErrorException() {
        super(DEFAULT_MESSAGE);
    }

    public InternalServerErrorException(String message) {
        super(message);
    }
}

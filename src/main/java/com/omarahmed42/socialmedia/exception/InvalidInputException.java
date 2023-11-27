package com.omarahmed42.socialmedia.exception;

public class InvalidInputException extends BadRequestException {
    public InvalidInputException(String message) {
        super(message);
    }
}

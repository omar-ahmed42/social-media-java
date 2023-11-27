package com.omarahmed42.socialmedia.exception;

public class ForbiddenNewsfeedAccessException extends ForbiddenException {
    private static final String DEFAULT_MESSAGE = "Forbidden: Cannot access newsfeed";

    public ForbiddenNewsfeedAccessException() {
        super(DEFAULT_MESSAGE);
    }

    public ForbiddenNewsfeedAccessException(String message) {
        super(message);
    }
}

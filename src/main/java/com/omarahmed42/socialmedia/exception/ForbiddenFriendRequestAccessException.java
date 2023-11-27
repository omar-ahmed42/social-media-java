package com.omarahmed42.socialmedia.exception;

public class ForbiddenFriendRequestAccessException extends ForbiddenException {
    private static final String DEFAULT_MESSAGE = "Forbidden: Cannot access friend request";

    public ForbiddenFriendRequestAccessException() {
        super(DEFAULT_MESSAGE);
    }

    public ForbiddenFriendRequestAccessException(String message) {
        super(message);
    }
}

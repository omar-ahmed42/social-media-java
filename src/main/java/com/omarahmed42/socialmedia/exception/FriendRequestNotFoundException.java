package com.omarahmed42.socialmedia.exception;

public class FriendRequestNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "Friend request not found";

    public FriendRequestNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public FriendRequestNotFoundException(String message) {
        super(message);
    }
}

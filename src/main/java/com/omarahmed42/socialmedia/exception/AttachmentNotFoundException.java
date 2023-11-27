package com.omarahmed42.socialmedia.exception;

public class AttachmentNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "Attachment not found";

    public AttachmentNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public AttachmentNotFoundException(String message) {
        super(message);
    }
}

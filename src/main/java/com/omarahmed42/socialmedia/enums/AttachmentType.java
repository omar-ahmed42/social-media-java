package com.omarahmed42.socialmedia.enums;

public enum AttachmentType {
    IMAGE("IMAGE", 1),
    VIDEO("VIDEO", 2);

    private final String text;
    private final int value;

    AttachmentType(final String text, final int value) {
        this.text = text;
        this.value = value;
    }

    @Override
    public String toString() {
        return text;
    }

    public int getValue() {
        return value;
    }
}

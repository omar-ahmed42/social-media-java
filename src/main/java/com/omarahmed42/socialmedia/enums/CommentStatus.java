package com.omarahmed42.socialmedia.enums;

public enum CommentStatus {
    DRAFT("DRAFT", 1),
    PUBLISHED("PUBLISHED", 2);

    private final String text;
    private final int value;

    CommentStatus(final String text, final int value) {
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

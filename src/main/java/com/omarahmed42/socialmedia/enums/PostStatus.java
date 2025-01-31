package com.omarahmed42.socialmedia.enums;

public enum PostStatus {
    DRAFT("DRAFT", 1),
    PUBLISHED("PUBLISHED", 2),
    ARCHIVED("ARCHIVED", 3),
    TOMBSTONE("TOMBSTONE", 4);

    private final String text;
    private final int value;

    PostStatus(final String text, final int value) {
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

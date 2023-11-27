package com.omarahmed42.socialmedia.enums;

public enum MessageStatus {
    DRAFT("DRAFT", 1),
    SENDING("SENDING", 2),
    SENT("SENT", 3),
    DELIVERED("DELIVERED", 4);

    private final String text;
    private final int value;

    MessageStatus(final String text, final int value) {
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

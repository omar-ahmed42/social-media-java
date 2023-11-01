package com.omarahmed42.socialmedia.enums;

public enum Roles {
    USER(1, "USER"),
    ADMIN(2, "ADMIN");

    private final String text;
    private final int value;

    Roles(final int value, final String text) {
        this.value = value;
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public int getValue() {
        return value;
    }
}

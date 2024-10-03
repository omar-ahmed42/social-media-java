package com.omarahmed42.socialmedia.enums;

public enum SortOrder {
    ASC("ASC", 1),
    DESC("DESC", 2);

    private final String text;
    private final int value;

    SortOrder(final String text, final int value) {
        this.text = text;
        this.value = value;
    }

    @Override
    public String toString() {
        return text;
    }

    public String text() {
        return toString();
    }

    public int value() {
        return value;
    }
}

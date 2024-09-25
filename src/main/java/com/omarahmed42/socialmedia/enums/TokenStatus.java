package com.omarahmed42.socialmedia.enums;

public enum TokenStatus {
    VALID("VALID"),
    INVALID("INVALID"),
    CONSUMED("CONSUMED"),
    COMPROMISED("COMPROMISED");

    private final String status;

    TokenStatus(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }

    public String status() {
        return this.status;
    }
}

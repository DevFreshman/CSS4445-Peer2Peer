// File: src/main/java/com/hoangtien/p2p/core/protocol/ErrorEnvelope.java
package com.ht.p2p.core.protocol;

import java.util.Objects;

public final class ErrorEnvelope {
    private final String code;
    private final String message;

    public ErrorEnvelope(String code, String message) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
        if (this.code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if (this.message.isBlank()) throw new IllegalArgumentException("message must not be blank");
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorEnvelope{code='" + code + "', message='" + message + "'}";
    }
}

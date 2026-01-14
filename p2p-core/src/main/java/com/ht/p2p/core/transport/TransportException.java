// File: src/main/java/com/ht/p2p/core/transport/TransportException.java
package com.ht.p2p.core.transport;

public class TransportException extends RuntimeException {
    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }
}

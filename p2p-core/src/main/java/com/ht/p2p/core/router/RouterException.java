// File: src/main/java/com/hoangtien/p2p/core/router/RouterException.java
package com.ht.p2p.core.router;

public class RouterException extends RuntimeException {
    public RouterException(String message) {
        super(message);
    }

    public RouterException(String message, Throwable cause) {
        super(message, cause);
    }
}

// File: src/main/java/com/ht/p2p/core/protocol/MessageType.java
package com.ht.p2p.core.protocol;

public enum MessageType {
    // Phase 2
    PING,
    PONG,
    ERROR,

    // Phase 4
    HELLO,
    HELLO_ACK
}

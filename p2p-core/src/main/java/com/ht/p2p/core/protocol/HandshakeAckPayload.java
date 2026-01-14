// File: src/main/java/com/ht/p2p/core/protocol/HandshakeAckPayload.java
package com.ht.p2p.core.protocol;

public record HandshakeAckPayload(
    String serverPeerId,
    String serverPublicKey,    // base64(X509)
    String nonce,
    long timestampEpochMs,
    String signature           // base64(Ed25519)
) {}

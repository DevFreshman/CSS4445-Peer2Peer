// File: src/main/java/com/ht/p2p/core/protocol/HandshakeInitPayload.java
package com.ht.p2p.core.protocol;

public record HandshakeInitPayload(
    String peerId,
    String publicKey,          // base64(X509)
    String nonce,              // base64(16 bytes)
    long timestampEpochMs,
    String signature           // base64(Ed25519)
) {}

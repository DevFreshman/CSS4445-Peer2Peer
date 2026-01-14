// File: src/main/java/com/ht/p2p/core/security/NodeIdentity.java
package com.ht.p2p.core.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Objects;

public final class NodeIdentity {
    private final String peerId;
    private final KeyPair keyPair;
    private final String publicKeyBase64;

    private NodeIdentity(String peerId, KeyPair keyPair, String publicKeyBase64) {
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        this.keyPair = Objects.requireNonNull(keyPair, "keyPair");
        this.publicKeyBase64 = Objects.requireNonNull(publicKeyBase64, "publicKeyBase64");
    }

    public static NodeIdentity generate(String peerId) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            KeyPair kp = kpg.generateKeyPair();
            String pubB64 = CryptoUtil.b64(kp.getPublic().getEncoded());
            return new NodeIdentity(peerId, kp, pubB64);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Ed25519 identity", e);
        }
    }

    public String peerId() {
        return peerId;
    }

    public KeyPair keyPair() {
        return keyPair;
    }

    public PublicKey publicKey() {
        return keyPair.getPublic();
    }

    public String publicKeyBase64() {
        return publicKeyBase64;
    }
}

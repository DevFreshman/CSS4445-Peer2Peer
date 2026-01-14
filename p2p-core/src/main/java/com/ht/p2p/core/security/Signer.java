// File: src/main/java/com/ht/p2p/core/security/Signer.java
package com.ht.p2p.core.security;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Objects;

public final class Signer {
    private Signer() {}

    public static String signBase64(String message, PrivateKey privateKey) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(privateKey, "privateKey");
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(privateKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] out = sig.sign();
            return CryptoUtil.b64(out);
        } catch (Exception e) {
            throw new IllegalStateException("Sign failed", e);
        }
    }
}

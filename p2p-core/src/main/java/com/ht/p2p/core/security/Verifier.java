// File: src/main/java/com/ht/p2p/core/security/Verifier.java
package com.ht.p2p.core.security;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Objects;

public final class Verifier {
    private Verifier() {}

    public static boolean verifyBase64(String message, String signatureBase64, PublicKey publicKey) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(signatureBase64, "signatureBase64");
        Objects.requireNonNull(publicKey, "publicKey");
        try {
            byte[] sigBytes = CryptoUtil.b64decode(signatureBase64);
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(publicKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            return sig.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }
}

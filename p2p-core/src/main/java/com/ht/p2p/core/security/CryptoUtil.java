// File: src/main/java/com/ht/p2p/core/security/CryptoUtil.java
package com.ht.p2p.core.security;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class CryptoUtil {
    private static final SecureRandom RNG = new SecureRandom();

    private CryptoUtil() {}

    public static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] b64decode(String b64) {
        return Base64.getDecoder().decode(b64);
    }

    public static String randomNonceBase64(int bytes) {
        byte[] buf = new byte[bytes];
        RNG.nextBytes(buf);
        return b64(buf);
    }

    public static String signingString(String peerId, String publicKeyBase64, String nonceBase64, long tsEpochMs) {
        // stable canonical form
        return peerId + "|" + publicKeyBase64 + "|" + nonceBase64 + "|" + tsEpochMs;
    }

    public static PublicKey publicKeyFromBase64(String publicKeyBase64) {
        try {
            byte[] enc = b64decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(enc);
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid publicKeyBase64", e);
        }
    }
}

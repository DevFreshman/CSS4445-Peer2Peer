// File: src/main/java/com/ht/p2p/core/service/security/NonceStore.java
package com.ht.p2p.core.service.security;

public interface NonceStore {
    /**
     * @return true if nonce was already seen (replay). If false, nonce is marked as seen.
     */
    boolean seenOrMark(String nonceBase64, long nowEpochMs);
}

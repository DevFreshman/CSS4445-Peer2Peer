// File: src/main/java/com/ht/p2p/core/peer/PeerStore.java
package com.ht.p2p.core.peer;

import java.util.Optional;

public interface PeerStore {
    void upsert(Peer peer);

    Optional<Peer> get(PeerId peerId);

    int size();
}

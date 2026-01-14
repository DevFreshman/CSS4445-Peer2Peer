// File: src/main/java/com/ht/p2p/core/peer/InMemoryPeerStore.java
package com.ht.p2p.core.peer;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPeerStore implements PeerStore {
    private final ConcurrentHashMap<String, Peer> peers = new ConcurrentHashMap<>();

    @Override
    public void upsert(Peer peer) {
        Objects.requireNonNull(peer, "peer");
        peers.merge(peer.peerId().value(), peer, (oldP, newP) -> {
            oldP.update(newP.address(), newP.lastSeen());
            return oldP;
        });
    }

    @Override
    public Optional<Peer> get(PeerId peerId) {
        Objects.requireNonNull(peerId, "peerId");
        return Optional.ofNullable(peers.get(peerId.value()));
    }

    @Override
    public int size() {
        return peers.size();
    }
}

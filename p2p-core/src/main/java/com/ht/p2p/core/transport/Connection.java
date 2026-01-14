// File: src/main/java/com/ht/p2p/core/transport/Connection.java
package com.ht.p2p.core.transport;

import com.ht.p2p.core.peer.Session;
import com.ht.p2p.core.protocol.Envelope;

import java.util.concurrent.CompletableFuture;

public interface Connection extends AutoCloseable {
    CompletableFuture<Envelope> send(Envelope request);

    Session getSession();

    String channelId();

    String remoteAddress();

    @Override
    void close();
}

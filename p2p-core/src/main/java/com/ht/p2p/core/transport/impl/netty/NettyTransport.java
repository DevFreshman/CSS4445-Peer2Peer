// File: src/main/java/com/ht/p2p/core/transport/impl/netty/NettyTransport.java
package com.ht.p2p.core.transport.impl.netty;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.peer.InMemoryPeerStore;
import com.ht.p2p.core.peer.PeerStore;
import com.ht.p2p.core.peer.SessionManager;
import com.ht.p2p.core.router.MessageRouter;
import com.ht.p2p.core.transport.Connection;
import com.ht.p2p.core.transport.Transport;

import java.util.Objects;

public final class NettyTransport implements Transport {

    @Override
    public NettyServer startServer(NodeContext ctx, int port, MessageRouter router) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(router, "router");
        // server owns its session manager + peer store by default
        NettyServer server = new NettyServer(ctx, port, router, new SessionManager(), new InMemoryPeerStore());
        server.start();
        return server;
    }

    @Override
    public Connection connect(NodeContext ctx, String host, int port) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(host, "host");
        NettyClient client = new NettyClient(ctx);
        return client.connect(host, port);
    }
}

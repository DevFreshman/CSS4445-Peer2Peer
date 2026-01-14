// File: src/main/java/com/ht/p2p/core/transport/Transport.java
package com.ht.p2p.core.transport;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.router.MessageRouter;

public interface Transport {

    /**
     * Start a TCP server bound to given port (0 = ephemeral).
     */
    AutoCloseable startServer(NodeContext ctx, int port, MessageRouter router);

    /**
     * Connect to remote TCP server.
     */
    Connection connect(NodeContext ctx, String host, int port);
}

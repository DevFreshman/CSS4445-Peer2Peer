// File: src/main/java/com/ht/p2p/Main.java
package com.ht.p2p;

import com.ht.p2p.core.Node;
import com.ht.p2p.core.NodeBuilder;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.observability.Logger;

/**
 * Demo Phase 0:
 * start -> sleep 1s -> stop
 * - Non-blocking start/stop (runs on event loop)
 * - No duplicated stop.request logs
 */
public final class Main {
    public static void main(String[] args) throws Exception {
        Node node = new NodeBuilder().build();
        Logger log = node.context().logger();

        log.info("demo.begin", Logger.fields(
            LogKeys.NODE_ID, node.context().nodeId()
        ));

        node.start();
        Thread.sleep(1_000);

        // Call stop exactly once for clean logs
        node.stop();

        // close() should be a no-op if STOPPING/STOPPED; keep it for safety
        node.close();

        log.info("demo.end", Logger.fields(
            LogKeys.NODE_ID, node.context().nodeId(),
            LogKeys.STATE, node.state()
        ));
    }
}

// File: src/main/java/com/hoangtien/p2p/core/Node.java
package com.ht.p2p.core;

import com.ht.p2p.core.lifecycle.LifecycleState;
import com.ht.p2p.core.lifecycle.NodeLifecycle;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.observability.Logger;

import java.util.Map;

/**
 * Phase 0 Node: bootstrap core only. No router/codec/transport/services.
 *
 * start()/stop():
 * - idempotent
 * - schedules work on event loop (Netty-friendly direction)
 * - does not block main thread for long
 */
public final class Node implements AutoCloseable {
    private final NodeContext ctx;
    private final NodeLifecycle lifecycle = new NodeLifecycle();

    public Node(NodeContext ctx) {
        this.ctx = ctx;
    }

    public NodeContext context() {
        return ctx;
    }

    public LifecycleState state() {
        return lifecycle.state();
    }

    public void start() {
        Logger log = ctx.logger();
        // Move state to RUNNING within lifecycle.start()
        lifecycle.start(() -> {
            // Ensure "start logic" runs on the event loop (non-blocking caller)
            ctx.eventLoop().execute(() -> {
                log.info("node.start",
                    Logger.fields(
                        LogKeys.NODE_ID, ctx.nodeId(),
                        LogKeys.STATE, lifecycle.state()
                    )
                );
                // Phase 0: no transport; just log that we are started.
                log.info("node.started",
                    Logger.fields(
                        LogKeys.NODE_ID, ctx.nodeId(),
                        LogKeys.STATE, lifecycle.state()
                    )
                );
            });
        });

        log.info("node.state_transition",
            Logger.fields(
                LogKeys.NODE_ID, ctx.nodeId(),
                LogKeys.TO, lifecycle.state()
            )
        );
    }

    public void stop() {
        Logger log = ctx.logger();
        lifecycle.stop(() -> {
            ctx.eventLoop().execute(() -> {
                log.info("node.stop",
                    Logger.fields(
                        LogKeys.NODE_ID, ctx.nodeId(),
                        LogKeys.STATE, lifecycle.state()
                    )
                );
                // Phase 0: shut down loop after "stop" logic enqueued
                log.info("node.stopping",
                    Logger.fields(
                        LogKeys.NODE_ID, ctx.nodeId(),
                        LogKeys.STATE, lifecycle.state()
                    )
                );
                ctx.eventLoop().shutdownGracefully();
            });
        });

        log.info("node.state_transition",
            Logger.fields(
                LogKeys.NODE_ID, ctx.nodeId(),
                LogKeys.TO, lifecycle.state()
            )
        );
    }

    @Override
    public void close() {
        // Idempotent stop is required.
        try {
            if (state() == LifecycleState.RUNNING) {
                stop();
            } else if (state() == LifecycleState.STOPPING || state() == LifecycleState.STOPPED) {
                // no-op
            } else if (state() == LifecycleState.NEW) {
                // no-op: close without start should not crash app; keep it gentle.
                ctx.logger().warn("node.close_called_while_new",
                    Map.of(LogKeys.NODE_ID, ctx.nodeId(), LogKeys.STATE, state()));
            } else {
                // STARTING/FAILED: keep strict? In Phase 0, just log.
                ctx.logger().warn("node.close_called_in_state",
                    Map.of(LogKeys.NODE_ID, ctx.nodeId(), LogKeys.STATE, state()));
            }
        } catch (RuntimeException e) {
            ctx.logger().error("node.close_error",
                Map.of(LogKeys.NODE_ID, ctx.nodeId(), LogKeys.STATE, state(), LogKeys.ERROR, e.toString()));
            throw e;
        }
    }
}

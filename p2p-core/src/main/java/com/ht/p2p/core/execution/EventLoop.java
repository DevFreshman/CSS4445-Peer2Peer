// File: src/main/java/com/hoangtien/p2p/core/execution/EventLoop.java
package com.ht.p2p.core.execution;

import java.util.concurrent.ScheduledFuture;

/**
 * Netty-friendly concept: single-threaded event loop abstraction.
 */
public interface EventLoop {
    void execute(Runnable task);

    ScheduledFuture<?> schedule(Runnable task, long delayMs);

    void shutdownGracefully();
}

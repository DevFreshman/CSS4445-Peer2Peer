// File: src/main/java/com/hoangtien/p2p/core/execution/NodeExecutor.java
package com.ht.p2p.core.execution;

import com.ht.p2p.core.util.Errors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 0 event loop implementation using a single-thread ScheduledExecutorService.
 * Thread name pattern: node-evloop-%d
 */
public final class NodeExecutor implements EventLoop {
    private final ScheduledExecutorService executor;

    public NodeExecutor() {
        this.executor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("node-evloop-"));
        // Avoid keeping cancelled tasks
        if (this.executor instanceof ScheduledThreadPoolExecutor stpe) {
            stpe.setRemoveOnCancelPolicy(true);
        }
    }

    @Override
    public void execute(Runnable task) {
        Errors.requireNonNull(task, "task");
        executor.execute(task);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delayMs) {
        Errors.requireNonNull(task, "task");
        if (delayMs < 0) throw Errors.illegalArg("delayMs must be >= 0");
        return executor.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdownGracefully() {
        executor.shutdown();
        try {
            // Keep short: Phase 0 shouldn't block long.
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger seq = new AtomicInteger(0);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            int id = seq.incrementAndGet();
            Thread t = new Thread(r, prefix + id);
            t.setDaemon(true);
            return t;
        }
    }
}

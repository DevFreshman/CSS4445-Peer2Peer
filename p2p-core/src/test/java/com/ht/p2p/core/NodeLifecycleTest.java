// File: src/test/java/com/ht/p2p/core/NodeLifecycleTest.java
package com.ht.p2p.core;

import com.ht.p2p.core.lifecycle.LifecycleState;
import com.ht.p2p.core.lifecycle.NodeLifecycle;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class NodeLifecycleTest {

    @Test
    void startThenRunning_stopThenStopped() {
        NodeLifecycle lc = new NodeLifecycle();
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();

        assertEquals(LifecycleState.NEW, lc.state());

        lc.start(starts::incrementAndGet);
        assertEquals(1, starts.get());
        assertEquals(LifecycleState.RUNNING, lc.state());

        lc.stop(stops::incrementAndGet);
        assertEquals(1, stops.get());
        assertEquals(LifecycleState.STOPPED, lc.state());
    }

    @Test
    void startIdempotent_runsOnStartOnce() {
        NodeLifecycle lc = new NodeLifecycle();
        AtomicInteger starts = new AtomicInteger();

        lc.start(starts::incrementAndGet);
        lc.start(starts::incrementAndGet);
        lc.start(starts::incrementAndGet);

        assertEquals(1, starts.get());
        assertEquals(LifecycleState.RUNNING, lc.state());
    }

    @Test
    void stopIdempotent_runsOnStopOnce() {
        NodeLifecycle lc = new NodeLifecycle();
        AtomicInteger stops = new AtomicInteger();

        lc.start(() -> {});
        lc.stop(stops::incrementAndGet);
        lc.stop(stops::incrementAndGet);
        lc.stop(stops::incrementAndGet);

        assertEquals(1, stops.get());
        assertEquals(LifecycleState.STOPPED, lc.state());
    }

    @Test
    void invalidTransitionThrows() {
        NodeLifecycle lc = new NodeLifecycle();

        // stop before start
        assertThrows(IllegalStateException.class, () -> lc.stop(() -> {}));

        // start -> stop -> start again is invalid in Phase 0 rules
        lc.start(() -> {});
        lc.stop(() -> {});
        assertThrows(IllegalStateException.class, () -> lc.start(() -> {}));
    }
}

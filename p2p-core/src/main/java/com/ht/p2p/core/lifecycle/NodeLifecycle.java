// File: src/main/java/com/hoangtien/p2p/core/lifecycle/NodeLifecycle.java
package com.ht.p2p.core.lifecycle;

import com.ht.p2p.core.util.Errors;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe lifecycle state machine for Node.
 *
 * Requirements:
 * - AtomicReference + small synchronized section for safe transitions
 * - start/stop idempotent
 * - onStart/onStop run exactly once
 */
public final class NodeLifecycle {
    private final AtomicReference<LifecycleState> state = new AtomicReference<>(LifecycleState.NEW);
    private final Object lock = new Object();

    public LifecycleState state() {
        return state.get();
    }

    /**
     * Allowed:
     * - NEW -> STARTING -> RUNNING (runs onStart exactly once)
     * Idempotent:
     * - STARTING/RUNNING: do nothing
     * Invalid:
     * - STOPPING/STOPPED/FAILED: throws
     */
    public void start(Runnable onStart) {
        Objects.requireNonNull(onStart, "onStart");

        LifecycleState s = state.get();
        if (s == LifecycleState.STARTING || s == LifecycleState.RUNNING) return;
        if (s == LifecycleState.STOPPING || s == LifecycleState.STOPPED || s == LifecycleState.FAILED) {
            throw Errors.illegalState("start() invalid from state " + s);
        }

        synchronized (lock) {
            LifecycleState cur = state.get();
            if (cur == LifecycleState.STARTING || cur == LifecycleState.RUNNING) return;
            if (cur != LifecycleState.NEW) {
                throw Errors.illegalState("start() invalid from state " + cur);
            }
            transition(cur, LifecycleState.STARTING);

            try {
                onStart.run();
                transition(LifecycleState.STARTING, LifecycleState.RUNNING);
            } catch (Throwable t) {
                // If start logic fails, mark FAILED.
                state.set(LifecycleState.FAILED);
                if (t instanceof RuntimeException re) throw re;
                throw new RuntimeException(t);
            }
        }
    }

    /**
     * Allowed:
     * - RUNNING -> STOPPING -> STOPPED (runs onStop exactly once)
     * Idempotent:
     * - STOPPING/STOPPED: do nothing
     * Invalid:
     * - NEW: throws (stop before start)
     * - FAILED: throws (explicit; can be changed later if you want to allow stop after fail)
     */
    public void stop(Runnable onStop) {
        Objects.requireNonNull(onStop, "onStop");

        LifecycleState s = state.get();
        if (s == LifecycleState.STOPPING || s == LifecycleState.STOPPED) return;
        if (s == LifecycleState.NEW) {
            throw Errors.illegalState("stop() invalid from state NEW");
        }
        if (s == LifecycleState.FAILED) {
            throw Errors.illegalState("stop() invalid from state FAILED");
        }
        if (s == LifecycleState.STARTING) {
            // We keep rules strict in Phase 0: STOP during STARTING is invalid.
            throw Errors.illegalState("stop() invalid from state STARTING");
        }

        synchronized (lock) {
            LifecycleState cur = state.get();
            if (cur == LifecycleState.STOPPING || cur == LifecycleState.STOPPED) return;
            if (cur != LifecycleState.RUNNING) {
                throw Errors.illegalState("stop() invalid from state " + cur);
            }

            transition(cur, LifecycleState.STOPPING);
            try {
                onStop.run();
                transition(LifecycleState.STOPPING, LifecycleState.STOPPED);
            } catch (Throwable t) {
                state.set(LifecycleState.FAILED);
                if (t instanceof RuntimeException re) throw re;
                throw new RuntimeException(t);
            }
        }
    }

    private void transition(LifecycleState expectedFrom, LifecycleState to) {
        LifecycleState cur = state.get();
        if (cur != expectedFrom) {
            throw Errors.illegalState("Invalid transition: expected " + expectedFrom + " but was " + cur);
        }
        state.set(to);
    }
}

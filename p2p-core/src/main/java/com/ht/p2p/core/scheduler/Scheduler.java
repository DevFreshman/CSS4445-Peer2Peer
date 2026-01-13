// com/ht/p2p/core/scheduler/Scheduler.java
package com.ht.p2p.core.scheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

public interface Scheduler extends AutoCloseable {
  ScheduledFuture<?> schedule(Duration delay, Runnable task);
  ScheduledFuture<?> scheduleAtFixedRate(Duration initialDelay, Duration period, Runnable task);
  @Override void close();
}

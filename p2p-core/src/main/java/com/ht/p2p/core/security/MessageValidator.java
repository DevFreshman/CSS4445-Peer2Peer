// com/ht/p2p/core/security/MessageValidator.java
package com.ht.p2p.core.security;

import com.ht.p2p.core.transport.Session;
import com.ht.p2p.proto.Envelope;

import java.time.Instant;

public final class MessageValidator {

  // basic anti-garbage checks
  public void validate(Session session, Envelope env) {
    if (env.getTimestampMs() <= 0) return; // allow 0 for now
    long now = Instant.now().toEpochMilli();
    long drift = Math.abs(now - env.getTimestampMs());
    // TODO: enforce drift window if you want (e.g. 5 minutes)
  }
}

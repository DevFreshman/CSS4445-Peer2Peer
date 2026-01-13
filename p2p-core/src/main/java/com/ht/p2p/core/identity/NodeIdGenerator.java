// com/ht/p2p/core/identity/NodeIdGenerator.java
package com.ht.p2p.core.identity;

import java.security.SecureRandom;

public final class NodeIdGenerator {
  private static final SecureRandom rnd = new SecureRandom();

  private NodeIdGenerator() {}

  public static NodeId random256() {
    byte[] b = new byte[32];
    rnd.nextBytes(b);
    return new NodeId(b);
  }
}

// com/ht/p2p/core/identity/NodeId.java
package com.ht.p2p.core.identity;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

public final class NodeId {
  private final byte[] bytes;

  public NodeId(byte[] bytes) {
    this.bytes = Objects.requireNonNull(bytes, "bytes").clone();
    if (this.bytes.length == 0) throw new IllegalArgumentException("NodeId empty");
  }

  public byte[] bytes() { return bytes.clone(); }

  public String hex() { return HexFormat.of().formatHex(bytes); }

  /** XOR distance (lexicographic comparable as unsigned bytes if needed) */
  public byte[] xor(NodeId other) {
    Objects.requireNonNull(other);
    int n = Math.min(this.bytes.length, other.bytes.length);
    byte[] out = new byte[n];
    for (int i = 0; i < n; i++) out[i] = (byte) (this.bytes[i] ^ other.bytes[i]);
    return out;
  }

  public static NodeId sha256(byte[] input) {
    try {
      var md = MessageDigest.getInstance("SHA-256");
      return new NodeId(md.digest(input));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override public boolean equals(Object o) {
    return (o instanceof NodeId n) && Arrays.equals(bytes, n.bytes);
  }
  @Override public int hashCode() { return Arrays.hashCode(bytes); }
  @Override public String toString() { return "NodeId(" + hex() + ")"; }
}

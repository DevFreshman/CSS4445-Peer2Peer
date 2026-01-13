package com.ht.p2p.net.peer;

import com.google.protobuf.ByteString;

import java.util.Objects;

public record Peer(String host, int port, ByteString nodeId) {

  public Peer {
    Objects.requireNonNull(host);
    if (port <= 0 || port > 65535) throw new IllegalArgumentException("Invalid port: " + port);
    // nodeId có thể null ở giai đoạn chưa handshake
  }

  public static Peer of(String host, int port) {
    return new Peer(host, port, null);
  }

  public String address() {
    return host + ":" + port;
  }
}

// com/ht/p2p/core/transport/Session.java
package com.ht.p2p.core.transport;

import com.ht.p2p.core.identity.NodeId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Session {
  private final String address; // host:port
  private volatile NodeId remoteId; // null until handshake ok
  private volatile boolean handshakeOk;

  private final Map<String, Object> attrs = new ConcurrentHashMap<>();

  public Session(String address) {
    this.address = address;
  }

  public String address() { return address; }

  public NodeId remoteId() { return remoteId; }
  public void remoteId(NodeId id) { this.remoteId = id; }

  public boolean handshakeOk() { return handshakeOk; }
  public void handshakeOk(boolean ok) { this.handshakeOk = ok; }

  public Map<String, Object> attrs() { return attrs; }
}

// com/ht/p2p/core/transport/TransportListener.java
package com.ht.p2p.core.transport;

import com.ht.p2p.proto.Envelope;

public interface TransportListener {
  void onConnected(Session session);
  void onDisconnected(Session session, Throwable cause);
  void onMessage(Session session, Envelope envelope);
}

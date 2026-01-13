// com/ht/p2p/core/transport/Transport.java
package com.ht.p2p.core.transport;

import com.ht.p2p.proto.Envelope;

import java.util.concurrent.CompletableFuture;

public interface Transport extends AutoCloseable {

  CompletableFuture<Void> startListener(int listenPort, TransportListener listener);

  CompletableFuture<Session> connect(String host, int port);

  void send(Session session, Envelope envelope);

  void close(Session session);

  @Override
  void close();
}

// com/ht/p2p/core/api/Node.java
package com.ht.p2p.core.api;

import com.ht.p2p.core.transport.Session;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Node extends AutoCloseable {

  CompletableFuture<Void> start();   // start transport listener (server)
  CompletableFuture<Void> stop();

  CompletableFuture<Session> connect(String host, int port);

  CompletableFuture<Boolean> ping(String host, int port, String message);

  CompletableFuture<Void> bootstrap(List<HostPort> seeds);

  CompletableFuture<Void> gossipPublish(String topic, byte[] payload);

  CompletableFuture<Void> dhtPut(byte[] key, byte[] value);

  CompletableFuture<byte[]> dhtGet(byte[] key);

  @Override
  void close();

  record HostPort(String host, int port) {}
}

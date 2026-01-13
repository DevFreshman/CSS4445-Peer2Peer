package com.ht.p2p.demo;

import com.ht.p2p.core.api.NodeConfig;
import com.ht.p2p.core.identity.NodeIdGenerator;
import com.ht.p2p.core.node.NodeImpl;
import com.ht.p2p.net.adapter.NettyTransport;

public final class Main {
  public static void main(String[] args) {
    var nodeA = new NodeImpl(
        NodeConfig.defaults(NodeIdGenerator.random256(), 9001),
        new NettyTransport()
    );
    nodeA.start().join();

    var nodeB = new NodeImpl(
        NodeConfig.defaults(NodeIdGenerator.random256(), 9002),
        new NettyTransport()
    );
    nodeB.start().join();

    boolean ok = nodeB.ping("127.0.0.1", 9001, "hello").join();
    System.out.println("PING OK = " + ok);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try { nodeB.close(); } catch (Throwable ignore) {}
      try { nodeA.close(); } catch (Throwable ignore) {}
    }));
  }
}

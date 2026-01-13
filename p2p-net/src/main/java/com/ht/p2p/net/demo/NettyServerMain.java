package com.ht.p2p.net.demo;

import com.google.protobuf.ByteString;
import com.ht.p2p.net.codec.P2PChannelInitializer;
import com.ht.p2p.net.handler.InboundEnvelopeHandler;
import com.ht.p2p.net.rpc.PendingRequests;
import com.ht.p2p.net.transport.NettyServer;

public final class NettyServerMain {
  public static void main(String[] args) throws Exception {
    int port = (args.length >= 1) ? Integer.parseInt(args[0]) : 9000;

    ByteString serverId = ByteString.copyFrom(new byte[]{0x01}); // tạm

    try (PendingRequests pending = new PendingRequests();
         NettyServer server = new NettyServer(
             port,
             new P2PChannelInitializer(() -> new InboundEnvelopeHandler(true, pending, serverId))
         )) {

      Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

      server.start();
      System.out.println("NettyServerMain is running on port " + port + " (Ctrl+C to stop)");
      Thread.currentThread().join();
    }
  }
}

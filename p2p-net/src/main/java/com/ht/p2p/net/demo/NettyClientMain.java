package com.ht.p2p.net.demo;

import com.google.protobuf.ByteString;
import com.ht.p2p.net.conn.ConnectionManager;
import com.ht.p2p.net.conn.HeartbeatService;
import com.ht.p2p.net.peer.Peer;
import com.ht.p2p.net.rpc.PendingRequests;
import com.ht.p2p.net.rpc.RpcClient;

import java.time.Duration;

public final class NettyClientMain {
  public static void main(String[] args) throws Exception {
    String host = (args.length >= 1) ? args[0] : "127.0.0.1";
    int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 9000;

    ByteString clientId = ByteString.copyFrom(new byte[]{0x02}); // tạm
    Peer server = Peer.of(host, port);

    try (PendingRequests pending = new PendingRequests();
         HeartbeatService hb = new HeartbeatService(pending, clientId);
         ConnectionManager cm = new ConnectionManager(pending, clientId, hb)) {

      RpcClient rpc = new RpcClient(pending, cm, clientId);

      rpc.ping(server, "ping", Duration.ofSeconds(2))
          .thenAccept(res ->
              System.out.println("[APP] ping result ok=" + res.getOk() + " msg=" + res.getMessage()))
          .exceptionally(err -> {
            System.out.println("[APP] ping failed: " + err);
            return null;
          });

      Thread.sleep(500);

      rpc.ping(server, "ping2", Duration.ofSeconds(2))
          .thenAccept(res ->
              System.out.println("[APP] ping2 result ok=" + res.getOk() + " msg=" + res.getMessage()))
          .exceptionally(err -> {
            System.out.println("[APP] ping2 failed: " + err);
            return null;
          });

      Thread.sleep(5000);
      System.out.println("Client exited.");
    }
  }
}

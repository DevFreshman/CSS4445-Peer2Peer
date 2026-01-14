// File: src/main/java/com/ht/p2p/Phase5MultiNodeTuiDemo.java
package com.ht.p2p;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.config.NodeConfig;
import com.ht.p2p.core.execution.NodeExecutor;
import com.ht.p2p.core.observability.Logger;
import com.ht.p2p.core.protocol.*;
import com.ht.p2p.core.router.MessageRouter;
import com.ht.p2p.core.router.RouteRegistry;
import com.ht.p2p.core.router.routes.RouteResult;
import com.ht.p2p.core.service.ping.PingService;
import com.ht.p2p.core.service.ping.PingServiceImpl;
import com.ht.p2p.core.transport.Connection;
import com.ht.p2p.core.transport.impl.netty.NettyServer;
import com.ht.p2p.core.transport.impl.netty.NettyTransport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class Phase5MultiNodeTuiDemo {

    private static final long RPC_TIMEOUT_MS = 2_000;

    private final Clock clock = Clock.systemUTC();
    private final NodeContext ctx;
    private final NettyTransport transport = new NettyTransport();

    private NettyServer server;
    private Integer serverPort;

    private final Map<String, Connection> conns = new ConcurrentHashMap<>();

    private String lastResult;

    public Phase5MultiNodeTuiDemo(String nodeId) {
        this.ctx = new NodeContext(
            nodeId,
            NodeConfig.defaults(),
            Logger.stdout(clock),
            clock,
            new NodeExecutor()
        );
    }

    public static void main(String[] args) throws Exception {
        String nodeId = args.length > 0 ? args[0] : "node";
        new Phase5MultiNodeTuiDemo(nodeId).run();
    }

    public void run() throws Exception {
        // Router: PING -> PONG
        RouteRegistry reg = new RouteRegistry();
        PingService ping = new PingServiceImpl();
        reg.register(MessageType.PING, (rctx, inbound) ->
            RouteResult.ok(ping.handlePing(rctx, inbound))
        );
        MessageRouter router = new MessageRouter(ctx, reg, List.of());

        printlnTitle();
        renderStatus();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                renderMenu();
                String line = br.readLine();
                if (line == null) break;

                String[] parts = line.trim().split("\\s+");
                if (parts.length == 0) continue;

                try {
                    switch (parts[0]) {
                        case "q" -> { lastResult = "Quit"; return; }

                        case "start" -> stepStartServer(router, Integer.parseInt(parts[1]));
                        case "connect" -> stepConnect(parts[1], Integer.parseInt(parts[2]));
                        case "peers" -> stepListPeers();
                        case "ping" -> stepPing(parts[1]);
                        case "close" -> stepClose(parts.length > 1 ? parts[1] : null);

                        default -> lastResult = "Unknown command";
                    }
                } catch (Exception e) {
                    lastResult = "ERROR: " + e.getMessage();
                }

                clear();
                printlnTitle();
                renderStatus();
            }
        } finally {
            shutdown();
        }
    }

    // ---------------- Commands ----------------

    private void stepStartServer(MessageRouter router, int port) {
        if (server != null) {
            lastResult = "Server already running";
            return;
        }
        server = transport.startServer(ctx, port, router);
        serverPort = server.localPort();
        lastResult = "Listening on port=" + serverPort;
    }

    private void stepConnect(String host, int port) throws Exception {
        Connection c = transport.connect(ctx, host, port);
        conns.put(c.channelId(), c);
        lastResult = "Connected to " + host + ":" + port + " channelId=" + c.channelId();
        sleep(80); // allow HELLO/ACK logs
    }

    private void stepListPeers() {
        if (conns.isEmpty()) {
            lastResult = "No peers";
            return;
        }
        StringBuilder sb = new StringBuilder("Peers:\n");
        conns.forEach((id, c) ->
            sb.append("  ").append(id)
              .append(" -> ").append(c.remoteAddress()).append("\n")
        );
        lastResult = sb.toString();
    }

    private void stepPing(String channelId) throws Exception {
        Connection c = conns.get(channelId);
        if (c == null) {
            lastResult = "No such channelId";
            return;
        }

        CorrelationId corr = CorrelationId.random();
        Envelope pingEnv = new Envelope(
            new Header(MessageType.PING, corr, clock.instant()),
            "PING"
        );

        long t0 = System.nanoTime();
        Envelope resp = c.send(pingEnv).get(RPC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        long t1 = System.nanoTime();

        lastResult = "PING -> " + resp.header().type()
            + " rttMs=" + ((t1 - t0) / 1_000_000);
    }

    private void stepClose(String channelId) {
        if (channelId == null) {
            conns.values().forEach(Connection::close);
            conns.clear();
            lastResult = "Closed all connections";
            return;
        }
        Connection c = conns.remove(channelId);
        if (c != null) {
            c.close();
            lastResult = "Closed " + channelId;
        } else {
            lastResult = "No such channelId";
        }
    }

    // ---------------- UI ----------------

    private void printlnTitle() {
        System.out.println("============================================================");
        System.out.println("   PHASE 5 – MULTI NODE P2P TUI (" + ctx.nodeId() + ")");
        System.out.println("============================================================");
    }

    private void renderMenu() {
        System.out.println();
        System.out.println("start <port>              start listening");
        System.out.println("connect <host> <port>     connect to peer");
        System.out.println("peers                     list peers");
        System.out.println("ping <channelId>          ping peer");
        System.out.println("close [channelId]         close one/all");
        System.out.println("q                         quit");
        System.out.print("\n> ");
    }

    private void renderStatus() {
        System.out.println();
        System.out.println("Node       : " + ctx.nodeId());
        System.out.println("Listening  : " + (server == null ? "NO" : serverPort));
        System.out.println("Peers      : " + conns.size());
        System.out.println("------------------------------------------------------------");
        System.out.println("Result     : " + (lastResult == null ? "-" : lastResult));
        System.out.println("------------------------------------------------------------");
    }

    private void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void shutdown() {
        conns.values().forEach(Connection::close);
        if (server != null) server.close();
        ctx.eventLoop().shutdownGracefully();
        clear();
        printlnTitle();
        System.out.println("\nBye!");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}

// File: src/main/java/com/ht/p2p/Phase4TuiDemo.java
package com.ht.p2p;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.config.NodeConfig;
import com.ht.p2p.core.execution.NodeExecutor;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.observability.Logger;
import com.ht.p2p.core.protocol.CorrelationId;
import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.protocol.Header;
import com.ht.p2p.core.protocol.MessageType;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Phase4TuiDemo {

    private static final long RPC_TIMEOUT_MS = 2_000;

    private final Clock clock = Clock.systemUTC();

    private final NodeContext serverCtx = new NodeContext(
        "server-node",
        NodeConfig.defaults(),
        Logger.stdout(clock),
        clock,
        new NodeExecutor()
    );

    private final NodeContext clientCtx = new NodeContext(
        "client-node",
        NodeConfig.defaults(),
        Logger.stdout(clock),
        clock,
        new NodeExecutor()
    );

    private final NettyTransport transport = new NettyTransport();

    private NettyServer server;
    private Connection conn;

    // status fields for UI
    private Integer serverPort;
    private String channelId;
    private String sessionId;
    private String remote;
    private boolean helloOk;
    private Long lastPingRttMs;
    private String lastCorr;
    private String lastResult;

    public static void main(String[] args) throws Exception {
        new Phase4TuiDemo().run();
    }

    public void run() throws Exception {
        // Build server router: PING -> PONG
        RouteRegistry reg = new RouteRegistry();
        PingService ping = new PingServiceImpl();
        reg.register(MessageType.PING, (rctx, inbound) -> RouteResult.ok(ping.handlePing(rctx, inbound)));
        MessageRouter router = new MessageRouter(serverCtx, reg, List.of());

        printlnTitle();
        renderStatus();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                renderMenu();
                String choice = br.readLine();
                if (choice == null) break;

                choice = choice.trim();
                if (choice.equalsIgnoreCase("q")) {
                    lastResult = "Quit requested";
                    break;
                }

                try {
                    switch (choice) {
                        case "1" -> stepStartServer(router);
                        case "2" -> stepConnectClient();
                        case "3" -> stepHello(); // HELLO is auto; this step validates and marks OK
                        case "4" -> stepPing();
                        case "5" -> stepClose();
                        case "6" -> stepReset(); // optional convenience
                        default -> lastResult = "Unknown choice: " + choice;
                    }
                } catch (Exception e) {
                    lastResult = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                    // also log structured error (nice for debug)
                    clientCtx.logger().error("phase4.tui.error", Map.of(
                        LogKeys.NODE_ID, clientCtx.nodeId(),
                        "err", String.valueOf(e)
                    ));
                }

                // update UI after each step
                clearScreenSoft();
                printlnTitle();
                renderStatus();
            }
        } finally {
            // always cleanup resources
            safeCloseConn();
            safeCloseServer();
            serverCtx.eventLoop().shutdownGracefully();
            clientCtx.eventLoop().shutdownGracefully();
            clearScreenSoft();
            printlnTitle();
            lastResult = (lastResult == null ? "Done" : lastResult);
            renderStatus();
            System.out.println("\nBye!");
        }
    }

    // ---------------- Steps ----------------

    private void stepStartServer(MessageRouter router) {
        if (server != null) {
            lastResult = "Server already started (idempotent)";
            return;
        }
        server = transport.startServer(serverCtx, 0, router);
        serverPort = server.localPort();
        lastResult = "Server started on port=" + serverPort;
    }

    private void stepConnectClient() throws Exception {
        if (server == null) {
            lastResult = "Start server first (step 1)";
            return;
        }
        if (conn != null) {
            lastResult = "Client already connected (idempotent)";
            return;
        }

        conn = transport.connect(clientCtx, "127.0.0.1", server.localPort());

        channelId = conn.channelId();
        remote = String.valueOf(conn.remoteAddress());
        var s = conn.getSession();
        sessionId = (s == null ? null : s.sessionId().value());

        lastResult = "Client connected (channelId=" + channelId + ")";
        // HELLO is auto on connect in your Phase 4; we give it a tiny moment to complete logs.
        sleepSilently(80);
    }

    private void stepHello() {
        if (conn == null) {
            lastResult = "Connect client first (step 2)";
            return;
        }

        // In your implementation, HELLO/ACK happens automatically.
        // We just mark it as OK if we have a session and channel.
        var s = conn.getSession();
        sessionId = (s == null ? null : s.sessionId().value());
        if (sessionId != null && channelId != null) {
            helloOk = true;
            lastResult = "HELLO handshake: OK (auto)";
        } else {
            helloOk = false;
            lastResult = "HELLO handshake: UNKNOWN (session not ready yet)";
        }
    }

    private void stepPing() throws Exception {
        if (conn == null) {
            lastResult = "Connect client first (step 2)";
            return;
        }

        Instant now = clock.instant();
        CorrelationId corr = CorrelationId.random();
        lastCorr = corr.value();

        Envelope pingEnv = new Envelope(new Header(MessageType.PING, corr, now), "PING");

        long t0 = System.nanoTime();
        Envelope resp = conn.send(pingEnv).get(RPC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        long t1 = System.nanoTime();

        lastPingRttMs = (t1 - t0) / 1_000_000;

        lastResult = "PING -> " + resp.header().type()
            + " payload=" + String.valueOf(resp.payload())
            + " rttMs=" + lastPingRttMs;
    }

    private void stepClose() {
        boolean did = false;

        if (conn != null) {
            safeCloseConn();
            did = true;
        }
        if (server != null) {
            safeCloseServer();
            did = true;
        }

        if (!did) lastResult = "Nothing to close";
        else lastResult = "Closed connection/server";
    }

    private void stepReset() {
        stepClose();
        // reset UI fields but keep lastResult from stepClose; then override:
        serverPort = null;
        channelId = null;
        sessionId = null;
        remote = null;
        helloOk = false;
        lastPingRttMs = null;
        lastCorr = null;
        lastResult = "Reset done (you can start again)";
    }

    // ---------------- UI ----------------

    private void printlnTitle() {
        System.out.println("============================================================");
        System.out.println("   P2P NETWORK LAYER – PHASE 4 TUI DEMO (HELLO + PING/PONG)  ");
        System.out.println("============================================================");
    }

    private void renderMenu() {
        System.out.println();
        System.out.println("[1] Start Server");
        System.out.println("[2] Connect Client");
        System.out.println("[3] HELLO (validate/mark OK)  *HELLO is auto on connect");
        System.out.println("[4] Send PING (await PONG)");
        System.out.println("[5] Close (conn + server)");
        System.out.println("[6] Reset (close + clear state)");
        System.out.println("[q] Quit");
        System.out.print("\nChoose: ");
    }

    private void renderStatus() {
        System.out.println();
        System.out.println("STATUS");
        System.out.println("------------------------------------------------------------");
        System.out.printf("Server     : %s%n", server == null ? "STOPPED" : ("RUNNING port=" + serverPort));
        System.out.printf("Client     : %s%n", conn == null ? "DISCONNECTED" : ("CONNECTED channelId=" + channelId));
        System.out.printf("Remote     : %s%n", remote == null ? "-" : remote);
        System.out.printf("SessionId  : %s%n", sessionId == null ? "-" : sessionId);
        System.out.printf("HELLO      : %s%n", helloOk ? "OK" : "NOT CONFIRMED");
        System.out.printf("LastCorr   : %s%n", lastCorr == null ? "-" : lastCorr);
        System.out.printf("LastPing   : %s%n", lastPingRttMs == null ? "-" : (lastPingRttMs + " ms"));
        System.out.println("------------------------------------------------------------");
        System.out.printf("Result     : %s%n", lastResult == null ? "-" : lastResult);
        System.out.println("------------------------------------------------------------");
    }

    private void clearScreenSoft() {
        // Works nicely on most terminals (Windows Terminal, VSCode terminal, etc.)
        // If it doesn't clear in your environment, it's still fine: the demo remains readable.
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // ---------------- Cleanup helpers ----------------

    private void safeCloseConn() {
        try {
            if (conn != null) conn.close();
        } catch (Exception ignored) {
        } finally {
            conn = null;
        }
    }

    private void safeCloseServer() {
        try {
            if (server != null) server.close();
        } catch (Exception ignored) {
        } finally {
            server = null;
        }
    }

    private static void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

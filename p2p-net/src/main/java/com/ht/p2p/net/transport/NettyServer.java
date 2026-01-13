package com.ht.p2p.net.transport;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP server wrapper: start/stop + bind port + attach ChannelInitializer.
 * This class MUST NOT contain protocol logic (Ping/Kademlia/etc).
 */
public final class NettyServer implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

  private final int port;
  private final ChannelInitializer<SocketChannel> childInitializer;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private Channel serverChannel;

  public NettyServer(int port, ChannelInitializer<SocketChannel> childInitializer) {
    this.port = port;
    this.childInitializer = childInitializer;
  }

  public synchronized void start() throws InterruptedException {
    if (serverChannel != null) {
      throw new IllegalStateException("Server already started");
    }

    // Boss: accept connections. Worker: handle IO.
    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup(); // default = 2 * CPU cores

    ServerBootstrap bootstrap = new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 1024)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childHandler(childInitializer);

    ChannelFuture bindFuture = bootstrap.bind(port).sync();
    serverChannel = bindFuture.channel();

    log.info("Server started, listening on {}", serverChannel.localAddress());
  }

  public synchronized void stop() {
    if (serverChannel == null) return;

    try {
      log.info("Stopping server...");
      serverChannel.close().syncUninterruptibly();
    } finally {
      serverChannel = null;

      if (bossGroup != null) bossGroup.shutdownGracefully().syncUninterruptibly();
      if (workerGroup != null) workerGroup.shutdownGracefully().syncUninterruptibly();

      bossGroup = null;
      workerGroup = null;
      log.info("Server stopped.");
    }
  }

  @Override
  public void close() {
    stop();
  }
}

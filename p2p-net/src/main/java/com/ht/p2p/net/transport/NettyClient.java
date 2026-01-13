package com.ht.p2p.net.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class NettyClient implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

  private final ChannelInitializer<SocketChannel> initializer;
  private final EventLoopGroup group;
  private final boolean ownsGroup;

  private Channel channel;

  /** Prefer this constructor: share group from ConnectionManager */
  public NettyClient(EventLoopGroup sharedGroup, ChannelInitializer<SocketChannel> initializer) {
    this.group = Objects.requireNonNull(sharedGroup);
    this.initializer = Objects.requireNonNull(initializer);
    this.ownsGroup = false;
  }

  /** Fallback: standalone (old behavior). Avoid for 100 peers. */
  public NettyClient(ChannelInitializer<SocketChannel> initializer) {
    this.group = new NioEventLoopGroup();
    this.initializer = Objects.requireNonNull(initializer);
    this.ownsGroup = true;
  }

  public synchronized void connect(String host, int port) throws InterruptedException {
    if (channel != null) throw new IllegalStateException("Client already connected");

    Bootstrap bootstrap = new Bootstrap()
        .group(group)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(initializer);

    ChannelFuture f = bootstrap.connect(host, port).sync();
    channel = f.channel();
    log.info("Client connected to {}:{}", host, port);
  }

  public synchronized Channel channel() {
    if (channel == null) throw new IllegalStateException("Not connected");
    return channel;
  }

  public synchronized void disconnect() {
    if (channel == null) return;
    try {
      log.info("Disconnecting client...");
      channel.close().syncUninterruptibly();
    } finally {
      channel = null;
      log.info("Client disconnected.");
    }
  }

  @Override
  public void close() {
    disconnect();
    if (ownsGroup) {
      group.shutdownGracefully().syncUninterruptibly();
    }
  }
}
  
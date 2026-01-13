package com.ht.p2p.net.adapter;

import com.ht.p2p.core.transport.Session;
import com.ht.p2p.core.transport.Transport;
import com.ht.p2p.core.transport.TransportListener;
import com.ht.p2p.net.transport.NettyServer; // dùng wrapper bạn đã có
import com.ht.p2p.proto.Envelope;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class NettyTransport implements Transport {

  private final EventLoopGroup clientGroup = new NioEventLoopGroup();
  private volatile TransportListener listener;
  private volatile NettyServer server;

  @Override
  public CompletableFuture<Void> startListener(int listenPort, TransportListener listener) {
    this.listener = Objects.requireNonNull(listener);

    // server dùng initializer forward về core
    var init = new CoreChannelInitializer(this.listener);
    this.server = new NettyServer(listenPort, init);

    try {
      server.start(); // bind port
      return CompletableFuture.completedFuture(null);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return CompletableFuture.failedFuture(e);
    }
  }

  @Override
  public CompletableFuture<Session> connect(String host, int port) {
    if (listener == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("TransportListener not set. Call startListener() first."));
    }

    CompletableFuture<Session> out = new CompletableFuture<>();

    Bootstrap b = new Bootstrap()
        .group(clientGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(new CoreChannelInitializer(listener));

    ChannelFuture f = b.connect(host, port);
    f.addListener((ChannelFutureListener) future -> {
      if (!future.isSuccess()) {
        out.completeExceptionally(future.cause());
        return;
      }
      Channel ch = future.channel();

      // tạo session nếu handler chưa tạo kịp
      Session s = ch.attr(NettyAttrs.CORE_SESSION).get();
      if (s == null) {
        s = new Session(host + ":" + port);
        s.attrs().put(NettyAttrs.ATTR_NETTY_CHANNEL, ch);
        ch.attr(NettyAttrs.CORE_SESSION).set(s);
      } else {
        s.attrs().put(NettyAttrs.ATTR_NETTY_CHANNEL, ch);
      }

      out.complete(s);
    });

    return out;
  }

  @Override
  public void send(Session session, Envelope envelope) {
    Objects.requireNonNull(session);
    Objects.requireNonNull(envelope);

    Object chObj = session.attrs().get(NettyAttrs.ATTR_NETTY_CHANNEL);
    if (!(chObj instanceof Channel ch)) {
      throw new IllegalStateException("No netty channel in session attrs for " + session.address());
    }
    ch.writeAndFlush(envelope);
  }

  @Override
  public void close(Session session) {
    Objects.requireNonNull(session);
    Object chObj = session.attrs().get(NettyAttrs.ATTR_NETTY_CHANNEL);
    if (chObj instanceof Channel ch) ch.close();
  }

  @Override
  public void close() {
    try {
      if (server != null) server.close();
    } catch (Throwable ignore) {
    } finally {
      server = null;
    }
    clientGroup.shutdownGracefully().syncUninterruptibly();
  }
}

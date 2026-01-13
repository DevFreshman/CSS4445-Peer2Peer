package com.ht.p2p.net.adapter;

import com.ht.p2p.core.transport.Session;
import com.ht.p2p.core.transport.TransportListener;
import com.ht.p2p.proto.Envelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketAddress;
import java.util.Objects;

final class ForwardToCoreHandler extends SimpleChannelInboundHandler<Envelope> {

  private final TransportListener listener;

  ForwardToCoreHandler(TransportListener listener) {
    this.listener = Objects.requireNonNull(listener);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    Session s = getOrCreateSession(ctx);
    listener.onConnected(s);
    ctx.fireChannelActive();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Envelope msg) {
    Session s = getOrCreateSession(ctx);
    listener.onMessage(s, msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    Session s = ctx.channel().attr(NettyAttrs.CORE_SESSION).get();
    if (s != null) listener.onDisconnected(s, null);
    ctx.fireChannelInactive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    Session s = ctx.channel().attr(NettyAttrs.CORE_SESSION).get();
    if (s != null) listener.onDisconnected(s, cause);
    ctx.close();
  }

  private Session getOrCreateSession(ChannelHandlerContext ctx) {
    var ch = ctx.channel();
    Session s = ch.attr(NettyAttrs.CORE_SESSION).get();
    if (s != null) return s;

    SocketAddress ra = ch.remoteAddress();
    String addr = (ra != null) ? ra.toString() : "unknown";

    // normalize "/127.0.0.1:12345" -> "127.0.0.1:12345" (nhìn cho sạch)
    if (addr.startsWith("/")) addr = addr.substring(1);

    s = new Session(addr);
    s.attrs().put(NettyAttrs.ATTR_NETTY_CHANNEL, ch);
    ch.attr(NettyAttrs.CORE_SESSION).set(s);
    return s;
  }
}

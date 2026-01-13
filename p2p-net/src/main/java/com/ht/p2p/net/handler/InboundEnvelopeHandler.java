package com.ht.p2p.net.handler;

import com.google.protobuf.ByteString;
import com.ht.p2p.net.rpc.PendingRequests;
import com.ht.p2p.proto.Envelope;
import com.ht.p2p.proto.PingReq;
import com.ht.p2p.proto.PingRes;
import com.ht.p2p.net.conn.ChannelAttrs;
import com.ht.p2p.proto.HelloReq;
import com.ht.p2p.proto.HelloRes;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.time.Instant;
import java.util.Objects;

public final class InboundEnvelopeHandler extends SimpleChannelInboundHandler<Envelope> {

  private final boolean isServerSide;
  private final PendingRequests pending; // nullable
  private final ByteString selfId;

  public InboundEnvelopeHandler(boolean isServerSide, PendingRequests pending, ByteString selfId) {
    this.isServerSide = isServerSide;
    this.pending = pending; // client side sẽ truyền vào
    this.selfId = Objects.requireNonNull(selfId);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Envelope env) {
    // HELLO response (client side)
if (env.hasHelloRes()) {
  HelloRes res = env.getHelloRes();
  ctx.channel().attr(ChannelAttrs.REMOTE_NODE_ID).set(res.getNodeId());
  if (pending != null) pending.complete(env.getRequestId(), env);
  System.out.println("[IN] HelloRes nodeId=" + res.getNodeId().size());
  return;
}

// HELLO request (server side)
if (env.hasHelloReq()) {
  HelloReq req = env.getHelloReq();
  ctx.channel().attr(ChannelAttrs.REMOTE_NODE_ID).set(req.getNodeId());

  if (isServerSide) {
    HelloRes res = HelloRes.newBuilder()
        .setOk(true)
        .setNodeId(selfId)
        .setVersion("p2p/0.1")
        .build();

    Envelope out = Envelope.newBuilder()
        .setSenderId(selfId)
        .setRequestId(env.getRequestId())
        .setTimestampMs(System.currentTimeMillis())
        .setHelloRes(res)
        .build();

    ctx.writeAndFlush(out);
    System.out.println("[OUT] HelloRes");
  }
  return;
}

    // 1) Response path: client nhận PingRes -> complete future
    if (env.hasPingRes()) {
      if (pending != null) {
        boolean matched = pending.complete(env.getRequestId(), env);
        if (!matched) {
          System.out.println("[IN] PingRes but no pending requestId=" + env.getRequestId());
        }
      }
      return;
    }

    // 2) Request path: server nhận PingReq -> trả PingRes
    if (env.hasPingReq()) {
      PingReq req = env.getPingReq();
      System.out.println("[IN] PingReq requestId=" + env.getRequestId() + " payload=" + req);

      if (isServerSide) {
        PingRes res = PingRes.newBuilder()
            .setOk(true)
            .setMessage("pong")
            .build();

        Envelope out = Envelope.newBuilder()
            .setSenderId(selfId)
            .setRequestId(env.getRequestId())
            .setTimestampMs(Instant.now().toEpochMilli())
            .setPingRes(res)
            .build();

        ctx.writeAndFlush(out);
        System.out.println("[OUT] PingRes requestId=" + env.getRequestId());
      }
      return;
    }

    System.out.println("[IN] Unknown envelope: " + env);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    // connection down -> fail all pending
    if (pending != null) {
    }
    ctx.fireChannelInactive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}

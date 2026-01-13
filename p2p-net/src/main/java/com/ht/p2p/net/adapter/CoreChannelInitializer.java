package com.ht.p2p.net.adapter;

import com.ht.p2p.core.transport.TransportListener;
import com.ht.p2p.proto.Envelope;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

import java.util.Objects;

final class CoreChannelInitializer extends ChannelInitializer<SocketChannel> {

  private static final int MAX_FRAME = 10 * 1024 * 1024;

  private final TransportListener listener;

  CoreChannelInitializer(TransportListener listener) {
    this.listener = Objects.requireNonNull(listener);
  }

  @Override
  protected void initChannel(SocketChannel ch) {
    ch.pipeline()
        .addLast("frameDecoder",
            new LengthFieldBasedFrameDecoder(
                MAX_FRAME,
                0, 4,
                0, 4
            ))
        .addLast("protobufDecoder", new ProtobufDecoder(Envelope.getDefaultInstance()))
        .addLast("frameEncoder", new LengthFieldPrepender(4))
        .addLast("protobufEncoder", new ProtobufEncoder())
        .addLast("toCore", new ForwardToCoreHandler(listener));
  }
}

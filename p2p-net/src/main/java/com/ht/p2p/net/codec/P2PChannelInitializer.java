package com.ht.p2p.net.codec;

import com.ht.p2p.net.handler.InboundEnvelopeHandler;
import com.ht.p2p.proto.Envelope;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * inbound : [LengthFieldBasedFrameDecoder] -> [ProtobufDecoder]
 * outbound: [ProtobufEncoder] -> [LengthFieldPrepender]
 *
 * NOTE: outbound order matters because Netty walks pipeline from tail->head.
 * So we must add LengthFieldPrepender BEFORE ProtobufEncoder,
 * so outbound becomes ProtobufEncoder -> LengthFieldPrepender.
 */
public final class P2PChannelInitializer extends ChannelInitializer<SocketChannel> {

  private static final int MAX_FRAME = 10 * 1024 * 1024; // 10MB

  private final Supplier<InboundEnvelopeHandler> inboundHandlerFactory;

  public P2PChannelInitializer(Supplier<InboundEnvelopeHandler> inboundHandlerFactory) {
    this.inboundHandlerFactory = Objects.requireNonNull(inboundHandlerFactory);
  }

  @Override
  protected void initChannel(SocketChannel ch) {
    ch.pipeline()
        // inbound: split frame by 4-byte length field
        .addLast("frameDecoder",
            new LengthFieldBasedFrameDecoder(
                MAX_FRAME,
                0,   // lengthFieldOffset
                4,   // lengthFieldLength
                0,   // lengthAdjustment
                4    // initialBytesToStrip
            ))
        .addLast("protobufDecoder", new ProtobufDecoder(Envelope.getDefaultInstance()))

        // ✅ outbound handlers MUST be added in this order:
        // (so outbound flow becomes ProtobufEncoder -> LengthFieldPrepender)
        .addLast("frameEncoder", new LengthFieldPrepender(4))
        .addLast("protobufEncoder", new ProtobufEncoder())

        // app handler (per-channel instance)
        .addLast("inbound", inboundHandlerFactory.get());
  }
}

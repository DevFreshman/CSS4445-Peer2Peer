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
 * Pipeline:
 * inbound:  [LengthFieldBasedFrameDecoder] -> [ProtobufDecoder(Envelope)] -> [InboundEnvelopeHandler]
 * outbound: [ProtobufEncoder] -> [LengthFieldPrepender(4)]
 *
 * IMPORTANT:
 * - Server có nhiều connection => InboundEnvelopeHandler phải tạo mới theo từng channel
 *   (nếu không sẽ dính lỗi "not @Sharable").
 */
public final class P2PChannelInitializer extends ChannelInitializer<SocketChannel> {

  private final Supplier<InboundEnvelopeHandler> inboundHandlerFactory;

  public P2PChannelInitializer(Supplier<InboundEnvelopeHandler> inboundHandlerFactory) {
    this.inboundHandlerFactory = Objects.requireNonNull(inboundHandlerFactory);
  }

  @Override
  protected void initChannel(SocketChannel ch) {
    ch.pipeline()
        .addLast("frameDecoder",
            new LengthFieldBasedFrameDecoder(
                10 * 1024 * 1024, // maxFrameLength 10MB (tạm)
                0,                // lengthFieldOffset
                4,                // lengthFieldLength
                0,                // lengthAdjustment
                4                 // initialBytesToStrip
            ))
        .addLast("protobufDecoder", new ProtobufDecoder(Envelope.getDefaultInstance()))
        .addLast("protobufEncoder", new ProtobufEncoder())
        .addLast("frameEncoder", new LengthFieldPrepender(4))
        // mỗi channel 1 handler instance
        .addLast("inbound", inboundHandlerFactory.get());
  }
}

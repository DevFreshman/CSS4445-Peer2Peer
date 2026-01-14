// File: src/main/java/com/ht/p2p/core/transport/impl/netty/EnvelopeEncoder.java
package com.ht.p2p.core.transport.impl.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.protocol.ErrorEnvelope;
import com.ht.p2p.core.protocol.Header;
import com.ht.p2p.core.protocol.HelloPayload;
import com.ht.p2p.core.protocol.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class EnvelopeEncoder extends MessageToByteEncoder<Envelope> {

    private final NodeContext ctx;
    private final ObjectMapper mapper;

    public EnvelopeEncoder(NodeContext ctx, ObjectMapper mapper) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    protected void encode(ChannelHandlerContext chx, Envelope msg, ByteBuf out) throws Exception {
        Envelope safe = normalize(msg);

        WireEnvelope wire = WireEnvelope.from(safe);
        byte[] json = mapper.writeValueAsBytes(wire);

        out.writeBytes(json);

        ctx.logger().info("netty.outbound", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "channelId", chx.channel().id().asShortText(),
            "type", String.valueOf(safe.header().type()),
            "corr", safe.header().correlationId().value(),
            "bytes", String.valueOf(json.length)
        ));
    }

    private Envelope normalize(Envelope in) {
        Object payload = in.payload();
        if (payload == null || payload instanceof String || payload instanceof ErrorEnvelope || payload instanceof HelloPayload) return in;

        Header h = new Header(
            MessageType.ERROR,
            in.header().correlationId(),
            Instant.now()
        );
        ErrorEnvelope err = new ErrorEnvelope("UNSUPPORTED_PAYLOAD", "Unsupported payload type: " + payload.getClass().getName());
        return new Envelope(h, err);
    }

    static final class WireEnvelope {
        public WireHeader header;
        public String payloadType;
        public Object payload;

        static WireEnvelope from(Envelope env) {
            WireEnvelope w = new WireEnvelope();
            w.header = WireHeader.from(env.header());

            Object p = env.payload();
            if (p == null) {
                w.payloadType = "null";
                w.payload = null;
            } else if (p instanceof String s) {
                w.payloadType = "string";
                w.payload = s;
            } else if (p instanceof HelloPayload hp) {
                w.payloadType = "hello";
                w.payload = Map.of("peerId", hp.peerId());
            } else if (p instanceof ErrorEnvelope e) {
                w.payloadType = "error";
                w.payload = Map.of("code", e.code(), "message", e.message());
            } else {
                w.payloadType = "error";
                w.payload = Map.of("code", "UNSUPPORTED_PAYLOAD", "message", "Unsupported payload type");
            }
            return w;
        }
    }

    static final class WireHeader {
        public String type;
        public String correlationId;
        public String timestamp;

        static WireHeader from(Header h) {
            WireHeader wh = new WireHeader();
            wh.type = h.type().name();
            wh.correlationId = h.correlationId().value();
            wh.timestamp = h.timestamp().toString();
            return wh;
        }
    }
}

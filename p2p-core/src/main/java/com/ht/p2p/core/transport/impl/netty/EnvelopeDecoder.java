// File: src/main/java/com/ht/p2p/core/transport/impl/netty/EnvelopeDecoder.java
package com.ht.p2p.core.transport.impl.netty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EnvelopeDecoder extends ByteToMessageDecoder {

    private final NodeContext ctx;
    private final ObjectMapper mapper;

    public EnvelopeDecoder(NodeContext ctx, ObjectMapper mapper) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    protected void decode(ChannelHandlerContext chx, ByteBuf in, List<Object> out) throws Exception {
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);

        Envelope env;
        try {
            JsonNode root = mapper.readTree(bytes);

            JsonNode header = root.get("header");
            MessageType type = MessageType.valueOf(header.get("type").asText());
            CorrelationId corr = CorrelationId.of(header.get("correlationId").asText());
            Instant ts = Instant.parse(header.get("timestamp").asText());

            Object payload = null;
            String payloadType = root.hasNonNull("payloadType") ? root.get("payloadType").asText() : "null";
            JsonNode payloadNode = root.get("payload");

            if ("string".equals(payloadType)) {
                payload = payloadNode == null || payloadNode.isNull() ? null : payloadNode.asText();
            } else if ("hello".equals(payloadType)) {
                String peerId = payloadNode != null && payloadNode.hasNonNull("peerId") ? payloadNode.get("peerId").asText() : "";
                payload = new HelloPayload(peerId);
            } else if ("error".equals(payloadType)) {
                String code = payloadNode != null && payloadNode.hasNonNull("code") ? payloadNode.get("code").asText() : "INTERNAL";
                String msg = payloadNode != null && payloadNode.hasNonNull("message") ? payloadNode.get("message").asText() : "Error";
                payload = new ErrorEnvelope(code, msg);
                type = MessageType.ERROR;
            } else if ("null".equals(payloadType)) {
                payload = null;
            } else {
                payload = new ErrorEnvelope("UNSUPPORTED_PAYLOAD", "Unsupported payloadType=" + payloadType);
                type = MessageType.ERROR;
            }

            env = new Envelope(new Header(type, corr, ts), payload);
        } catch (Exception e) {
            Header h = new Header(MessageType.ERROR, CorrelationId.random(), Instant.now());
            env = new Envelope(h, new ErrorEnvelope("DECODE_ERROR", e.toString()));
        }

        ctx.logger().info("netty.inbound", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "channelId", chx.channel().id().asShortText(),
            "type", String.valueOf(env.header().type()),
            "corr", env.header().correlationId().value(),
            "bytes", String.valueOf(bytes.length)
        ));

        out.add(env);
    }
}

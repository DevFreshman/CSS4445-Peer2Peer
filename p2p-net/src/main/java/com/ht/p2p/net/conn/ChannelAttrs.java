package com.ht.p2p.net.conn;

import com.google.protobuf.ByteString;
import io.netty.util.AttributeKey;

public final class ChannelAttrs {
  private ChannelAttrs() {}

  public static final AttributeKey<ByteString> REMOTE_NODE_ID =
      AttributeKey.valueOf("REMOTE_NODE_ID");

  public static final AttributeKey<Boolean> HANDSHAKE_OK =
      AttributeKey.valueOf("HANDSHAKE_OK");
}

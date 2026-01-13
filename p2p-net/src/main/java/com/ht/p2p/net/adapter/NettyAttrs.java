package com.ht.p2p.net.adapter;

import com.ht.p2p.core.transport.Session;
import io.netty.util.AttributeKey;

final class NettyAttrs {
  private NettyAttrs() {}

  static final AttributeKey<Session> CORE_SESSION =
      AttributeKey.valueOf("CORE_SESSION");

  static final String ATTR_NETTY_CHANNEL = "netty.channel";
}

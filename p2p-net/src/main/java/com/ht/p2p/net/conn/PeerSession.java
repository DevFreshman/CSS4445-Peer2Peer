package com.ht.p2p.net.conn;

import com.google.protobuf.ByteString;
import io.netty.channel.Channel;

public record PeerSession(String address, ByteString remoteNodeId, Channel channel) {}

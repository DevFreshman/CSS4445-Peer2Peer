// File: src/main/java/com/ht/p2p/core/transport/impl/netty/SessionChannelKeys.java
package com.ht.p2p.core.transport.impl.netty;

import com.ht.p2p.core.peer.Session;
import io.netty.util.AttributeKey;

public final class SessionChannelKeys {
    private SessionChannelKeys() {}

    public static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("p2p.session");
}

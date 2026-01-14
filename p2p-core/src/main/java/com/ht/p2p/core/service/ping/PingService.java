// File: src/main/java/com/hoangtien/p2p/core/service/ping/PingService.java
package com.ht.p2p.core.service.ping;

import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.router.routes.RouteContext;

public interface PingService {
    Envelope handlePing(RouteContext ctx, Envelope ping);
}

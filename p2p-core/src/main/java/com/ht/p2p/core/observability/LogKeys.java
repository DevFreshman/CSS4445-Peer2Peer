// File: src/main/java/com/hoangtien/p2p/core/observability/LogKeys.java
package com.ht.p2p.core.observability;

/**
 * Structured log keys: key=value (stdout).
 */
public final class LogKeys {
    private LogKeys() {}

    public static final String TS = "ts";
    public static final String LVL = "lvl";
    public static final String MSG = "msg";

    public static final String NODE_ID = "nodeId";
    public static final String STATE = "state";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String ERROR = "error";
    public static final String THREAD = "thread";
}

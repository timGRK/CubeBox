package com.cube.core.common;

import io.netty.util.AttributeKey;

import java.util.concurrent.ScheduledFuture;

import com.cube.core.conn.Connection;

public class SysConst {

    /**
     * connenction key
     */
    public static final AttributeKey<Connection> CONN_KEY = AttributeKey.valueOf("CONN_KEY");
    /**
     * 安全验证key
     */
    public static final AttributeKey<String> SECURE_KEY = AttributeKey.valueOf("SECURE_KEY");
    /**
     * mac地址key
     */
    public static final AttributeKey<String> MAC_KEY = AttributeKey.valueOf("MAC_KEY");
    /**
     * wifi模块版本key
     */
    public static final AttributeKey<String> WIFI_VER_KEY = AttributeKey.valueOf("WIFI_VER_KEY");
    /**
     * mcu模块版本key
     */
    public static final AttributeKey<String> MCU_VER_KEY = AttributeKey.valueOf("MCU_VER_KEY");
    
    /**
     * 延迟开关
     */
    public static final AttributeKey<ScheduledFuture<?>> DELAY_KEY = AttributeKey.valueOf("DELAY_KEY");
}

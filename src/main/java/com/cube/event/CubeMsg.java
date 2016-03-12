package com.cube.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.core.conn.Connection;
import com.cube.core.conn.ConnectionManager;

import io.netty.channel.ChannelHandlerContext;

public class CubeMsg {

    private static final Logger LOG = LoggerFactory.getLogger(CubeMsg.class);

    private EventEnum type;
    private ChannelHandlerContext ctx;
    private byte[] data;

    public EventEnum getType() {
        return type;
    }

    public void setType(EventEnum type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    /**
     * 获取CubeMsg并赋值ChannelHandlerContext
     * 返回null则为无效mac
     */
    public static CubeMsg buildMsg(String mac) {
        Connection conn = ConnectionManager.getInstance().getConn(mac);
        if (conn == null) {
            LOG.info("mac:{}对应的conn为空", mac);
            return null;
        }
        CubeMsg cubeMsg = new CubeMsg();
        cubeMsg.setCtx(conn.getCtx());
        return cubeMsg;
    }

}

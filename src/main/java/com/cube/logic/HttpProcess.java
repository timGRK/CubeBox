package com.cube.logic;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface HttpProcess {

    public void execute(ChannelHandlerContext ctx, FullHttpRequest req);
}

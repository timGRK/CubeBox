package com.cube.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import org.springframework.stereotype.Component;

@Component("CubeChannelInit")
public class CubeChannelInit extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LoggingHandler(LogLevel.INFO));
        pipeline.addLast(new IdleStateHandler(40, 0, 0));
        pipeline.addLast("LengthFieldBasedFrameDecoder", new LengthFieldBasedFrameDecoder(128, 2, 2));
        pipeline.addLast("TransforDecoder", new TransforDecoder());
        pipeline.addLast("cubehandler", new CubeInboundHandler());
        pipeline.addLast("TransforEncoder", new TransforEncoder());
    }

}

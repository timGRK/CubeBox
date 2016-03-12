package com.cube.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import com.cube.event.CubeMsg;
import com.cube.event.EventEnum;

public class TransforDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            ctx.pipeline().close();
            return;
        }
        CubeMsg event = new CubeMsg();
        event.setCtx(ctx);
        event.setType(EventEnum.valuesOf(in.readShort()));
        // 读取长度
        short length = in.readShort();
        byte[] data = new byte[length];
        in.readBytes(data);
        event.setData(data);
        out.add(event);
    }

}

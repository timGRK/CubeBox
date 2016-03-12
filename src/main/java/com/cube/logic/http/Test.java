package com.cube.logic.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.handler.HttpServerInboundHandler;
import com.cube.logic.DefaultHttpProcess;

public class Test extends DefaultHttpProcess {

    private static final Logger LOG = LoggerFactory.getLogger(Test.class);

    @Override
    protected void doExcute(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp,
            Map<String, List<String>> params, HttpPostRequestDecoder decoder) {
        ByteBuf content = null;
        try {
            byte[] ccc = new byte[req.content().readableBytes()];
            LOG.info("ccc length:{}==========", req.content().readableBytes());
            req.content().readBytes(ccc);
            LOG.info("req.content:{}", new String(ccc));

            // LOG.info("params:abc@{}", params.get("abc").get(0));
            // LOG.info("params:bbb@{}", params.get("bbb").get(0));

            content =
                    Unpooled.copiedBuffer("这是测试，this is a  test" + HttpServerInboundHandler.NEWLINE,
                            CharsetUtil.UTF_8);
            LOG.info("content size: {}===============================", content.readableBytes());
            resp.content().capacity(content.readableBytes()).writeBytes(content);
            resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");

        } finally {
            if (content != null) {
                ReferenceCountUtil.release(content);
            }
        }

    }

}

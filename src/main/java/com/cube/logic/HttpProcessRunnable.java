package com.cube.logic;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ReferenceCountUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProcessRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(HttpProcessRunnable.class);
    public static final Map<String, HttpProcess> ROUTE = new HashMap<String, HttpProcess>();
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private ChannelHandlerContext ctx;
    private FullHttpRequest req;

    private boolean retain = false;

    public HttpProcessRunnable(ChannelHandlerContext ctx, FullHttpRequest req) {
        super();
        this.ctx = ctx;
        this.req = req;
        req.retain();
        retain = true;
    }

    @Override
    public void run() {
        try {
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.getUri());
            String path = queryStringDecoder.path();
            HttpProcess process = ROUTE.get(path);
            if (process == null) {
                LOG.info("404 at {}", req.getUri());
                FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                res.setStatus(HttpResponseStatus.NOT_FOUND);
                res.content().writeBytes("404无效资源".getBytes());
                HttpHeaders.setContentLength(res, res.content().readableBytes());
                res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
                ctx.channel().writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
            } else {
                process.execute(ctx, req);
            }
        } catch (Exception e) {
            LOG.error("执行http业务", e);
            ctx.close();
        } finally {
            if (retain) {
                ReferenceCountUtil.release(req);
                retain = false;
            }
            LOG.info("after run ref release req:{}=========", req.refCnt());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (retain) {
                ReferenceCountUtil.release(req);
                retain = false;
            }
            LOG.info("after finalize ref release req:{}=========", req.refCnt());
        } finally {
            super.finalize();
        }
    }

}

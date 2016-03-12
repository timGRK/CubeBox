package com.cube.logic.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.core.conn.ConnectionManager;
import com.cube.logic.DefaultHttpProcess;

/**
 * 查看有多少LED在线
 * @description ViewLeds
 * @author cgg
 * @version 0.1
 * @date 2014年8月16日
 */
public class ViewLeds extends DefaultHttpProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ViewLeds.class);

    @Override
    protected void doExcute(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp,
            Map<String, List<String>> params, HttpPostRequestDecoder decoder) {
        try {
            LOG.info("开始处理查看有多少LED在线");
            List<String> connLst = ConnectionManager.getInstance().listAllConn();
            for (String s : connLst) {
                resp.content().writeBytes(s.getBytes());
                resp.content().writeBytes("<br />".getBytes());
            }
            resp.content().writeBytes(("共有" + connLst.size() + "个led连接").getBytes());
            resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
        } finally {
        }

    }

}

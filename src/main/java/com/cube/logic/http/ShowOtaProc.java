package com.cube.logic.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.logic.DefaultHttpProcess;
import com.cube.logic.proc.OTAReplyProc;

/**
 * 查看OTA升级结果
 * @description ShowOtaProc
 * @author cgg
 * @version 0.1
 * @date 2014年8月16日
 */
public class ShowOtaProc extends DefaultHttpProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ShowOtaProc.class);
    private static final String OUT_CONTENT = "OTA执行结果,成功/总数:{0}/{1}";

    @Override
    protected void doExcute(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp,
            Map<String, List<String>> params, HttpPostRequestDecoder decoder) {
        try {
            LOG.info("查看OTA升级结果");
            if (OTAReplyProc.TOTAL.get() == 0) {
                resp.content().writeBytes(MessageFormat.format(OUT_CONTENT, 0, 0).getBytes());
            } else {
                resp.content().writeBytes(
                        MessageFormat.format(OUT_CONTENT, OTAReplyProc.SUCCESS.get(), OTAReplyProc.TOTAL.get())
                                .getBytes());
            }
            resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
        } finally {
        }
    }

}

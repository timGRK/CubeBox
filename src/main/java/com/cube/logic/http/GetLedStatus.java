package com.cube.logic.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.event.EventEnum;
import com.cube.event.ReplyEvent;
import com.cube.logic.DefaultHttpProcess;
import com.cube.server.CubeBootstrap;
import com.cube.utils.MD5FileUtil;
import com.cube.utils.NetUtils;

/**
 * 查询led状态
 * @description GetLedStatus
 * @author cgg
 * @version 0.1
 * @date 2014年8月16日
 */
public class GetLedStatus extends DefaultHttpProcess {

    private static final Logger LOG = LoggerFactory.getLogger(GetLedStatus.class);

    // public final static byte[] GET_LED_STATUS_COMMAND = new byte[] {(byte)
    // 0xBB, 0x00, 0x3A, 0x30, 0x32, 0x30, 0x34,
    // 0x30, 0x30, 0x46, 0x41, 0x0D};
    public final static byte[] GET_LED_STATUS_COMMAND = new byte[] {(byte) 0xBB, (byte) 0xdd, 0x0f, 0x3a, 0x30,
            0x32, 0x30, 0x30, 0x30, 0x34, 0x30, 0x30, 0x30, 0x30, 0x46, 0x41, 0x0d, 0x0a};

    @Override
    protected void doExcute(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp,
            Map<String, List<String>> params, HttpPostRequestDecoder decoder) throws IOException {
        ByteBuf buf = null;
        ReplyEvent replyEvent = null;
        try {
            LOG.info("开始处理获取LED状态");
            String mac = null;
            if (method(req) == HttpMethod.GET) {
                mac = getParam("mac", params);
                LOG.info("method GET 获得mac");
            } else {
                InterfaceHttpData data = getHttpData("mac", decoder);
                mac = parseData(data);
                LOG.info("method POST 获得mac");
            }
            if (StringUtils.isBlank(mac)) {
                LOG.info("获取的mac为空");
                resp.content().writeBytes("mac地址不能为空".getBytes());
                resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
                return;
            }
            if (!NetUtils.validateMac(mac)) {
                LOG.info("获取的mac非法");
                resp.content().writeBytes((mac + ",mac地址格式非法").getBytes());
                resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
                return;
            }
            LOG.info("mac是:{}", mac);
            CubeMsg cubeMsg = CubeMsg.buildMsg(mac);
            if (cubeMsg == null) {
                LOG.info("mac:{}无有效连接", mac);
                sendUnableConn(resp);
                return;
            }
            replyEvent = new ReplyEvent();
            cubeMsg.setType(EventEnum.THREE);

            buf = Unpooled.buffer(4 + GET_LED_STATUS_COMMAND.length);
            buf.writeInt(replyEvent.getId());
            buf.writeBytes(GET_LED_STATUS_COMMAND);

            CubeBootstrap.processRunnable.putReply(replyEvent);

            cubeMsg.setData(buf.array());

            CubeBootstrap.processRunnable.pushUpMsg(cubeMsg);
            synchronized (replyEvent) {
                try {
                    if (replyEvent.getObj() == null) {
                        replyEvent.wait(10000);
                    }

                    replyEvent = CubeBootstrap.processRunnable.getReply(replyEvent.getId());
                    if (replyEvent == null || replyEvent.getObj() == null) {
                        LOG.info("mac:{},timeout", mac);
                        sendTimeout(resp);
                    } else {
                        // TODO 灯的状态返回处理
                        byte[] retData = (byte[]) replyEvent.getObj();
                        if (retData.length == 0) {
                            resp.content().writeInt(0);
                        } else {
                            resp.content().writeBytes(MD5FileUtil.bufferToHex(retData).getBytes());
                        }
                        resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
                    }
                } catch (InterruptedException e) {
                    LOG.info("等待结束mac:{}", mac);
                    sendTimeout(resp);
                }
            }

        } finally {
            if (buf != null) {
                ReferenceCountUtil.release(buf);
            }
            if (replyEvent != null) {
                CubeBootstrap.processRunnable.removeReply(replyEvent.getId());
            }
        }

    }

}

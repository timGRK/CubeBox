package com.cube.logic.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.event.EventEnum;
import com.cube.event.ReplyEvent;
import com.cube.logic.DefaultHttpProcess;
import com.cube.server.CubeBootstrap;
import com.cube.utils.NetUtils;

/**
 * 设置LED状态
 * @description SetLedStatus
 * @author cgg
 * @version 0.1
 * @date 2014年8月16日
 */
public class SetLedStatus extends DefaultHttpProcess {

    private static final Logger LOG = LoggerFactory.getLogger(SetLedStatus.class);

    // public final static byte[] C1 = new byte[] {(byte) 0xbb, (byte) 0x00,
    // (byte) 0x3a, (byte) 0x30, (byte) 0x31,
    // (byte) 0x30, (byte) 0x31, (byte) 0x30, (byte) 0x31, (byte) 0x46, (byte)
    // 0x44, (byte) 0x0d, (byte) 0x0a};
    // public final static byte[] C2 = new byte[] {(byte) 0xbb, (byte) 0x00,
    // (byte) 0x3a, (byte) 0x30, (byte) 0x31,
    // (byte) 0x30, (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x46, (byte)
    // 0x45, (byte) 0x0d, (byte) 0x0a};
    // public final static byte[] C3 = new byte[] {(byte) 0xbb, (byte) 0x00,
    // (byte) 0x3a, (byte) 0x30, (byte) 0x31,
    // (byte) 0x30, (byte) 0x33, (byte) 0x30, (byte) 0x30, (byte) 0x46, (byte)
    // 0x43, (byte) 0x0d, (byte) 0x0a};
    // public final static byte[] C4 = new byte[] {(byte) 0xbb, (byte) 0x00,
    // (byte) 0x3a, (byte) 0x30, (byte) 0x31,
    // (byte) 0x30, (byte) 0x32, (byte) 0x30, (byte) 0x31, (byte) 0x46, (byte)
    // 0x43, (byte) 0x0d, (byte) 0x0a};

    public final static byte[] C1 = new byte[] {(byte) 0xbb, (byte) 0xdd, (byte) 0x0f, (byte) 0x3a, (byte) 0x30,
            (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30,
            0x30, 0x46, 0x45, 0x0d, 0x0a};
    public final static byte[] C2 = new byte[] {(byte) 0xbb, (byte) 0xdd, (byte) 0x0f, (byte) 0x3a, (byte) 0x30,
            (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30,
            0x31, 0x46, 0x44, 0x0d, 0x0a};
    public final static byte[] C3 = new byte[] {(byte) 0xbb, (byte) 0xdd, (byte) 0x0f, (byte) 0x3a, (byte) 0x30,
            (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x32, (byte) 0x30, (byte) 0x30, (byte) 0x30,
            0x31, 0x46, 0x43, 0x0d, 0x0a};
    public final static byte[] C4 = new byte[] {(byte) 0xbb, (byte) 0xdd, (byte) 0x0f, (byte) 0x3a, (byte) 0x30,
            (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x33, (byte) 0x30, (byte) 0x30, (byte) 0x30,
            0x30, 0x46, 0x43, 0x0d, 0x0a};

    @Override
    protected void doExcute(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp,
            Map<String, List<String>> params, HttpPostRequestDecoder decoder) throws IOException {
        ByteBuf buf = null;
        ReplyEvent replyEvent = null;
        try {
            LOG.info("开始处理设置LED状态");
            String macs = null;
            String status = null;
            if (method(req) == HttpMethod.GET) {
                macs = getParam("mac", params);
                status = getParam("status", params);
                LOG.info("method GET 获得mac");
                LOG.info("method GET 获得status");
            } else {
                InterfaceHttpData macData = getHttpData("mac", decoder);
                InterfaceHttpData statusData = getHttpData("status", decoder);
                macs = parseData(macData);
                status = parseData(statusData);
                LOG.info("method POST 获得mac");
                LOG.info("method POST 获得status");
            }
            if (StringUtils.isBlank(macs)) {
                LOG.info("获取的mac为空");
                sendParamsError("mac地址不能为空", resp);
                return;
            }
            if (StringUtils.isBlank(status)) {
                LOG.info("获取的status为空");
                sendParamsError("status不能为空", resp);
                return;
            }
            macs = macs.trim();
            String[] arrayMac = macs.split(",");
            for(String strMac : arrayMac){
            	if (!NetUtils.validateMac(strMac.trim())) {
            		LOG.info("获取的mac非法");
            		sendParamsError(strMac + ",mac地址格式非法", resp);
            		return;
            	}
            }
            LOG.info("mac是:{}", macs);
            
            //=============多个mac
            if(arrayMac.length > 1){
            	//多个mac地址
            	for(String mac : arrayMac){
            		CubeMsg cubeMsg = CubeMsg.buildMsg(mac.trim());
            		if (cubeMsg == null) {
                        LOG.info("mac:{}无有效连接", mac);
                        sendUnableConn(resp);
                        return;
                    }
            		cubeMsg.setType(EventEnum.THREE);
            		replyEvent = new ReplyEvent();
            		buf = Unpooled.buffer(4 + C1.length);
                    buf.writeInt(replyEvent.getId());
                    if ("1".equals(status)) {
                        buf.writeBytes(C1);
                    } else if ("2".equals(status)) {
                        buf.writeBytes(C2);
                    } else if ("3".equals(status)) {
                        buf.writeBytes(C3);
                    } else if ("4".equals(status)) {
                        buf.writeBytes(C4);
                    } else {
                        sendParamsError("status无效值:" + status, resp);
                        return;
                    }

                    cubeMsg.setData(buf.array());
                    CubeBootstrap.processRunnable.pushUpMsg(cubeMsg);
                    ReferenceCountUtil.release(buf);
            	}
            	buf = null;
            	return;
            }
            
            
            
            String mac = arrayMac[0].trim();
            CubeMsg cubeMsg = CubeMsg.buildMsg(mac);
            if (cubeMsg == null) {
                LOG.info("mac:{}无有效连接", mac);
                sendUnableConn(resp);
                return;
            }
            replyEvent = new ReplyEvent();
            cubeMsg.setType(EventEnum.THREE);

            buf = Unpooled.buffer(4 + C1.length);
            buf.writeInt(replyEvent.getId());
            if ("1".equals(status)) {
                buf.writeBytes(C1);
            } else if ("2".equals(status)) {
                buf.writeBytes(C2);
            } else if ("3".equals(status)) {
                buf.writeBytes(C3);
            } else if ("4".equals(status)) {
                buf.writeBytes(C4);
            } else {
                sendParamsError("status无效值:" + status, resp);
                return;
            }

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

                        byte[] retData = (byte[]) replyEvent.getObj();
                        if (retData.length == 0) {
                            resp.content().writeInt(0);
                        } else if (retData[0] == 0x00) {
                            resp.content().writeBytes("0".getBytes());
                        } else {
                            resp.content().writeBytes("1".getBytes());
                        }

                        resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");

                        ScheduledFuture<?> oldFuture = SetDelay.futureMap.get(mac);
                        if (oldFuture != null) {
                            LOG.info("mac:{}关闭定时关灯任务", mac);
                            oldFuture.cancel(false);
                        }
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

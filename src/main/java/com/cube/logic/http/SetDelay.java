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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.event.EventEnum;
import com.cube.event.ReplyEvent;
import com.cube.logic.DefaultHttpProcess;
import com.cube.logic.HttpProcessRunnable;
import com.cube.server.CubeBootstrap;
import com.cube.utils.NetUtils;

/**
 * 设置延迟控制
 * @description SetDelay
 * @author cgg
 * @version 0.1
 * @date 2014年8月16日
 */
public class SetDelay extends DefaultHttpProcess {

    private static final Logger LOG = LoggerFactory.getLogger(SetDelay.class);

    // private static final byte[] DELAY_COMMAND = new byte[] {(byte) 0xBB,
    // 0x00, 0x3A, 0x30, 0x31, 0x30, 0x34, 0x32,
    // 0x31, 0x44, 0x41, 0x0D, 0x0A};
    private static final byte[] DELAY_COMMAND = new byte[] {(byte) 0xBB, (byte) 0xdd, 0x0f, 0x3a, 0x30, 0x31, 0x30,
            0x30, 0x30, 0x34, 0x30, 0x30, 0x31, 0x31, 0x45, 0x41, 0x0d, 0x0a};

    public static ConcurrentHashMap<String, ScheduledFuture<?>> futureMap =
            new ConcurrentHashMap<String, ScheduledFuture<?>>();

    @Override
    protected void doExcute(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp,
            Map<String, List<String>> params, HttpPostRequestDecoder decoder) throws IOException {
        ByteBuf buf = null;
        ReplyEvent replyEvent = null;
        try {
            LOG.info("设置延迟控制");
            String mac = null;
            String time = null;
            if (method(req) == HttpMethod.GET) {
                mac = getParam("mac", params);
                time = getParam("time", params);
                LOG.info("method GET 获得mac");
                LOG.info("method GET 获得time");
            } else {
                InterfaceHttpData macData = getHttpData("mac", decoder);
                InterfaceHttpData timeData = getHttpData("time", decoder);
                mac = parseData(macData);
                time = parseData(timeData);
                LOG.info("method POST 获得mac");
                LOG.info("method POST 获得time");
            }
            if (StringUtils.isBlank(mac)) {
                LOG.info("获取的mac为空");
                sendParamsError("mac地址不能为空", resp);
                return;
            }
            if (StringUtils.isBlank(time)) {
                LOG.info("获取的time为空");
                sendParamsError("time不能为空", resp);
                return;
            }
            if (!NetUtils.validateMac(mac)) {
                LOG.info("获取的mac非法");
                sendParamsError(mac + ",mac地址格式非法", resp);
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

            buf = Unpooled.buffer(4 + DELAY_COMMAND.length);
            buf.writeInt(replyEvent.getId());
            buf.writeBytes(DELAY_COMMAND);

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

                        // 设置延迟
                        String[] ts = time.split(":");
                        int hour = Integer.valueOf(ts[0]);
                        int min = Integer.valueOf(ts[1]);
                        int sec = Integer.valueOf(ts[2]);
                        long delay = 0L;
                        if (hour >= 0 && hour < 7) {
                            delay += 3600 * (7 - hour);
                        }
                        if (hour >= 7) {
                            delay += 24 * 3600 - (hour - 7) * 3600;
                        }
                        delay -= min * 60;
                        delay -= sec;
                        DelayCloseLedThread runnable = new DelayCloseLedThread(mac);
                        LOG.info("设置定时mac:{}, delay:{} seconds", mac, delay);
                        ScheduledFuture<?> future =
                                cubeMsg.getCtx().executor().schedule(runnable, delay, TimeUnit.SECONDS);

                        ScheduledFuture<?> oldFuture = futureMap.put(mac, future);
                        if (oldFuture != null) {
                            LOG.info("mac:{}关闭老的定时任务", mac);
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

    static class DelayCloseLedThread implements Runnable {

        public final static byte[] C1 = new byte[] {(byte) 0xbb, (byte) 0xdd, (byte) 0x0f, (byte) 0x3a, (byte) 0x30,
            (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30,
            0x30, 0x46, 0x45, 0x0d, 0x0a};
        private final String mac;

        public DelayCloseLedThread(String mac) {
            this.mac = mac;
        }

        public void run() {
            ByteBuf buf = null;
            try {
                LOG.info("早晨定时关灯开始");
                CubeMsg cubeMsg = CubeMsg.buildMsg(mac);
                if (cubeMsg == null) {
                    LOG.info("连接无效mack:{}", mac);
                    return;
                }
                ReplyEvent replyEvent = new ReplyEvent();
                cubeMsg.setType(EventEnum.THREE);
                buf = Unpooled.buffer(4 + C1.length);
                buf.writeInt(replyEvent.getId());
                buf.writeBytes(C1);
                cubeMsg.setData(buf.array());
                CubeBootstrap.processRunnable.pushUpMsg(cubeMsg);
                // 非线程安全，需要以后修改
                SetDelay.futureMap.remove(mac);
            } catch (Exception e) {
                LOG.info("定时早晨关灯异常", e);
            } finally {
                if (buf != null) {
                    ReferenceCountUtil.release(buf);
                }
            }
        }

    }

}

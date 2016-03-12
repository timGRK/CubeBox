package com.cube.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.core.common.SysConst;
import com.cube.core.conn.Connection;
import com.cube.core.conn.ConnectionManager;
import com.cube.event.CubeMsg;
import com.cube.event.EventEnum;
import com.cube.server.CubeBootstrap;
import com.cube.utils.ByteBufUtils;
import com.cube.utils.CommUtils;

@Sharable
public class CubeInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(CubeInboundHandler.class);
    // 用来生成随机key
    private static final char[] dic = new char[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static final int LENGTH = 32;

    /**
     * 随机获取key
     */
    private String randomKey(int length) {
        Random random = new Random(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder("");
        int index = 0;
        for (int i = 0; i < length; i++) {
            index = random.nextInt(dic.length);
            sb.append(dic[index]);
        }
        return sb.toString();
    }

    /**
     * 如果5秒没有验证通过，关闭连接
     * @description DelayClose
     * @author cgg
     * @version 0.1
     * @date 2014年8月16日
     */
    class DelayClose implements Runnable {

        private ChannelHandlerContext ctx;

        public DelayClose(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            Attribute<ScheduledFuture<?>> futureAttr = ctx.channel().attr(SysConst.DELAY_KEY);
            if(futureAttr.get() != null){
                futureAttr.remove();
            }
            if (!ctx.channel().isRegistered()) {
                LOG.info("{}检查5秒内有没有通过验证，连接已经被unRegistered", ctx.channel().toString());
                return;
            }
            String mac = CommUtils.getMacFromAttr(ctx);
            if (StringUtils.isBlank(mac)) {
                // 没有验证通过，关闭连接
                LOG.info("{} at 5秒内没有通过验证，关闭连接", ctx.channel().toString());
                ctx.pipeline().close();
            }
        }

    }

    /*
     * class SendKey implements Runnable { public ChannelHandlerContext ctx;
     * public ByteBuf buf;
     * @Override public void run() { if (ctx.channel().isWritable()) {
     * ctx.channel().writeAndFlush(buf); } else {
     * ReferenceCountUtil.release(buf); } } }
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // 新的Connection
        Connection conn = ConnectionManager.getInstance().getNewConnection(ctx);
        Attribute<Connection> connAttr = ctx.channel().attr(SysConst.CONN_KEY);
        connAttr.set(conn);
        LOG.info("register a channel and a connection========");
        // 生成随机的key，用来验证连接合法性
        String key = randomKey(LENGTH);
        Attribute<String> secAttr = ctx.channel().attr(SysConst.SECURE_KEY);
        secAttr.set(key);
        // 生成待发送的key
        ByteBuf buf = ByteBufUtils.str2Buf(key);
        ByteBuf frame = ByteBufUtils.toFrameBuf(EventEnum.ONE.getVal(), buf);
        ReferenceCountUtil.release(buf);
        // 5秒没有验证通过，关闭连接
        DelayClose delayClose = new DelayClose(ctx);
        ScheduledFuture<?> future = ctx.executor().schedule(delayClose, 5, TimeUnit.SECONDS);
        Attribute<ScheduledFuture<?>> futureAttr = ctx.channel().attr(SysConst.DELAY_KEY);
        if(futureAttr.get() != null){
            futureAttr.get().cancel(false);
        }
        futureAttr.set(future);

        // 需要在当前线程马上发送
        ctx.channel().writeAndFlush(frame);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        Attribute<Connection> attr = ctx.channel().attr(SysConst.CONN_KEY);
        Connection conn = attr.get();
        if (StringUtils.isNotBlank(conn.getMac())) {
            LOG.info("remove a connetion:{}, from connectionmanager", conn.getMac());
            ConnectionManager.getInstance().removeConn(conn.getMac());
        }
        Attribute<ScheduledFuture<?>> futureAttr = ctx.channel().attr(SysConst.DELAY_KEY);
        if(futureAttr.get() != null){
            LOG.info("remove future");
            futureAttr.get().cancel(false);
            futureAttr.remove();
        }
        LOG.info("remove a connection======");
        attr.remove();
        ctx.channel().attr(SysConst.MAC_KEY).remove();
        ctx.channel().attr(SysConst.SECURE_KEY).remove();
        ctx.channel().attr(SysConst.WIFI_VER_KEY).remove();
        ctx.channel().attr(SysConst.MCU_VER_KEY).remove();

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof CubeMsg) {
                String mac = CommUtils.getMacFromAttr(ctx);
                // handshake不需要验证
                if (((CubeMsg) msg).getType() != EventEnum.ONE && StringUtils.isBlank(mac)) {
                    LOG.info("连接未验证");
                    ctx.pipeline().close();
                    return;
                }
                CubeBootstrap.processRunnable.pushUpMsg((CubeMsg) msg);
            } else {
                LOG.error("error Object in channelRead:{}", msg.toString());
            }

        } finally {
            // 如果是CubeMsg，不是ReferenceCounted，将不会release
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                // 空闲时间过久，关闭连接
                LOG.info("空闲超过心跳事件，断开连接.mac:{}", CommUtils.getMacFromAttr(ctx));
                ctx.pipeline().close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("CubeInboundHanlder异常", cause);
        LOG.info("close channel");
        ctx.pipeline().close();
    }

}

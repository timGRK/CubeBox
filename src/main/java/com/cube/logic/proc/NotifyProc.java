package com.cube.logic.proc;

import io.netty.buffer.ByteBuf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.exception.IllegalDataException;
import com.cube.logic.Process;
import com.cube.utils.ByteBufUtils;

/**
 * Serve2LED通知业务
 * @description
 * @author cgg
 * @version 0.1
 * @date 2014年8月6日
 */
public class NotifyProc implements Process {

    private static final Logger LOG = LoggerFactory.getLogger(NotifyProc.class);

    

    
    /**
     * 要发送数据，必须对CubeMsg设置ctx和data , ctx 从ConnectionManager中获取, data必须包含replyid
     * ReplyEvent可以向ProcessRunnable注册 然后ReplyEvent.wait(timeout);
     * 在finally中删除ProcessRunnable的注册
     */
    @Override
    public void excute(CubeMsg msg) throws IllegalDataException {

        LOG.info("主动通知:{}", new String(msg.getData()));
        ByteBuf frame = ByteBufUtils.toFrameBuf(msg.getType().getVal(), msg.getData());
        if (msg.getCtx().channel().isWritable()) {
            LOG.info("向设备发送数据 ");
            msg.getCtx().pipeline().writeAndFlush(frame);
        } else {
            LOG.info("设备不可写，无法发送数据,关闭连接");
            msg.getCtx().pipeline().close();
        }
    }
}

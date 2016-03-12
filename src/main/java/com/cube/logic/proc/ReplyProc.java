package com.cube.logic.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.event.ReplyEvent;
import com.cube.exception.IllegalDataException;
import com.cube.logic.Process;
import com.cube.server.CubeBootstrap;
import com.cube.utils.CommUtils;

/**
 * LED2Serve业务处理
 * @description HandShake
 * @author cgg
 * @version 0.1
 * @date 2014年8月6日
 */
public class ReplyProc implements Process {

    private static final Logger LOG = LoggerFactory.getLogger(ReplyProc.class);

    @Override
    public void excute(CubeMsg msg) throws IllegalDataException {
        LOG.info("收到LED回答:{}", new String(msg.getData()));
        Integer replyId = CommUtils.getReplyIdFromByte(msg.getData());
        LOG.info("replyId:{}", replyId);
        ReplyEvent replyEvent = CubeBootstrap.processRunnable.getReply(replyId);
        if (replyEvent == null) {
            LOG.info("replyId:{}，对应的replyEvent对象已经过期!", replyId);
            return;
        }
        replyEvent.setObj(CommUtils.getReplyObjDataFromByte(msg.getData()));
        synchronized (replyEvent) {
            replyEvent.notifyAll();
        }
    }
}

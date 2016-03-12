package com.cube.logic.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.exception.IllegalDataException;
import com.cube.logic.Process;
import com.cube.utils.CommUtils;
import com.cube.utils.NetUtils;

/**
 * 心跳包处理
 * @description 
 * @author cgg
 * @version 0.1
 * @date 2014年8月6日
 */
public class HeartBeat implements Process {

    private static final Logger LOG = LoggerFactory.getLogger(HeartBeat.class);

    @Override
    public void excute(CubeMsg msg) throws IllegalDataException {
       LOG.info("收到心跳mac:{}", CommUtils.getMacFromAttr(msg.getCtx()));
       NetUtils.sendHeartBeat(msg.getCtx());
    }

}

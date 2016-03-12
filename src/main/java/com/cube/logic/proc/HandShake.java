package com.cube.logic.proc;

import io.netty.util.Attribute;

import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.core.common.SysConst;
import com.cube.core.conn.Connection;
import com.cube.core.conn.ConnectionManager;
import com.cube.event.CubeMsg;
import com.cube.exception.IllegalDataException;
import com.cube.logic.Process;
import com.cube.utils.CommUtils;
import com.cube.utils.NetUtils;
import com.cube.utils.SecureUtils;

/**
 * 握手
 * @description HandShake
 * @author cgg
 * @version 0.1
 * @date 2014年8月6日
 */
public class HandShake implements Process {

    private static final Logger LOG = LoggerFactory.getLogger(HandShake.class);

    @Override
    public void excute(CubeMsg msg) throws IllegalDataException {
        LOG.info("data:{}", new String(msg.getData()));
        String mac = CommUtils.getMacFromByte(msg.getData());
        Connection conn = ConnectionManager.getInstance().getConn(mac);
        if(conn != null){
            LOG.info("已经验证通过,传输异常====mac:{}", conn.getMac());
            msg.getCtx().pipeline().close();
            return;
        }
        String key = CommUtils.getKeyFromAttr(msg.getCtx());
        String checksum = SecureUtils.encode(key, mac);
        LOG.info("checksum:{}", checksum);
        String clientChecksum = CommUtils.getChecksumFromByte(msg.getData());
        if (checksum.equalsIgnoreCase(clientChecksum)) {
            LOG.info("验证通过mac:{}", mac);
            CommUtils.setMacAttr(msg.getCtx(), mac);
            // 注册connection
            conn = CommUtils.getConn(msg.getCtx());
            boolean ret = conn.setMac(mac);
            if(!ret){
                //mac设置失败
                LOG.info("conn.setMac失败:{}", mac);
                NetUtils.sendUnpass(msg.getCtx());
                return;
            }
            ConnectionManager.getInstance().addToConns(conn.getMac(), conn);
            //取消5秒验证检查
            Attribute<ScheduledFuture<?>> futureAttr = msg.getCtx().channel().attr(SysConst.DELAY_KEY);
            if(futureAttr.get() != null){
                LOG.info("{}验证成功，取消5秒验证检查", msg.getCtx().channel().toString());
                futureAttr.get().cancel(false);
            }
            // send pass
            NetUtils.sendPass(msg.getCtx());
        } else {
            //验证失败
            NetUtils.sendUnpass(msg.getCtx());
        }

    }

}

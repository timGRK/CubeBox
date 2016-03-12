package com.cube.logic.proc;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.exception.IllegalDataException;
import com.cube.logic.Process;
import com.cube.utils.MD5FileUtil;

/**
 * OTA回复
 * @description OTAReplyProc
 * @author cgg
 * @version 0.1
 * @date 2014年8月17日
 */
public class OTAReplyProc implements Process {

    private static final Logger LOG = LoggerFactory.getLogger(OTAReplyProc.class);
    public static volatile AtomicInteger SUCCESS = new AtomicInteger(0);
    public static volatile AtomicInteger TOTAL = new AtomicInteger(0);

    @Override
    public void excute(CubeMsg msg) throws IllegalDataException {
        LOG.info("收到LED的OTA回答:{}", MD5FileUtil.bufferToHex(msg.getData()));
        /*if (msg.getData().length != 18) {
            LOG.info("{}@OTA回复内容长度错误,关闭连接", msg.getCtx().channel().toString());
            msg.getCtx().channel().close();
            return;
        }
        String mac = new String(msg.getData(), 0, 17);
        LOG.info("{}@OTA回复mac:{}", msg.getCtx().channel().toString(), mac);
        // TODO OTA回复处理
        if (msg.getData()[17] == 1) {
            int success = SUCCESS.incrementAndGet();
            LOG.info("OTA成功加以{}/{}", success, TOTAL);
        } else {
            LOG.info("{}OTA没有成功mac:{}", msg.getCtx().channel().toString(), mac);
        }
*/
    }
}

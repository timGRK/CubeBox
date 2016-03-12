package com.cube.logic;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cube.event.EventEnum;
import com.cube.logic.proc.HandShake;
import com.cube.logic.proc.HeartBeat;
import com.cube.logic.proc.NotifyProc;
import com.cube.logic.proc.OTAReplyProc;
import com.cube.logic.proc.RegVersion;
import com.cube.logic.proc.ReplyProc;
import com.cube.logic.proc.RomUpdateProc;

@Component
public class ProcessManager {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessManager.class);
    private static ProcessManager INSTANCE;

    public Map<EventEnum, Process> processMap = new HashMap<EventEnum, Process>();

    public static ProcessManager getInstance() {
        return INSTANCE;
    }

    public Process getProcess(EventEnum eventEnum) {
        return processMap.get(eventEnum);
    }

    /**
     * 初始化处理过程 在此处添加处理接口
     */
    @PostConstruct
    private void init() {
        LOG.info("init the INSTANCE");
        INSTANCE = new ProcessManager();
        // 连接验证
        INSTANCE.processMap.put(EventEnum.ONE, new HandShake());
        // 注册版本号
        INSTANCE.processMap.put(EventEnum.TWO, new RegVersion());
        // 云->LED
        INSTANCE.processMap.put(EventEnum.THREE, new NotifyProc());
        // LED->云应答
        INSTANCE.processMap.put(EventEnum.FOUR, new ReplyProc());
        // 心跳
        INSTANCE.processMap.put(EventEnum.FIVE, new HeartBeat());
        // OTA回复处理
        INSTANCE.processMap.put(EventEnum.SIX, new OTAReplyProc());
        //ROM升级
        INSTANCE.processMap.put(EventEnum.SEVEN, new RomUpdateProc());
    }
}

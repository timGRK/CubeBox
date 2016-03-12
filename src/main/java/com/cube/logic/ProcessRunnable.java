package com.cube.logic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.event.EventEnum;
import com.cube.event.ReplyEvent;

/**
 * 工作对象
 * @description Worker
 * @author cgg
 * @version 0.1
 * @date 2014年8月6日
 */
public class ProcessRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessRunnable.class);

    private ConcurrentHashMap<Integer, ReplyEvent> replyMap = new ConcurrentHashMap<Integer, ReplyEvent>();

    // 任务队列
    private ConcurrentLinkedQueue<CubeMsg> workqueue = new ConcurrentLinkedQueue<CubeMsg>();

    private volatile boolean isRunning = false;

    public void putReply(ReplyEvent reply) {
        replyMap.put(reply.getId(), reply);
    }

    public ReplyEvent getReply(Integer id) {
        return replyMap.get(id);
    }

    public ReplyEvent removeReply(Integer id) {
        return replyMap.remove(id);
    }

    /**
     * 向事件队列中添加事件
     */
    public boolean pushUpMsg(CubeMsg msg) {
        if (isRunning) {
            synchronized (workqueue) {
                boolean ret = workqueue.add(msg);
                if(ret){
                    workqueue.notifyAll();
                }
                return ret;
            }
        } else {
            return false;
        }
    }

    public ProcessRunnable() {
        isRunning = false;
    }

    @Override
    public void run() {
        LOG.info("worker starting...");
        isRunning = true;
        while (true) {
            try {
                CubeMsg msg = null;
                synchronized (workqueue) {
                    msg = workqueue.poll();
                    if(msg == null){
                        workqueue.wait();
                        continue;
                    }
                }
                
//                if (msg == null) {
                    // 如果没有数据，则sleep1秒钟。如果使用wait,
                    // 对workqueue操作时，需要而外的锁，造成向workqueue写时竞争锁
                    // LOG.info("workqueue is empty, sleep 2 seconds.");
//                    Thread.sleep(1000);
                    // LOG.info("work wake up, after 2seconds.continue");
//                    continue;
//                }

                // 具体工作
                EventEnum event = msg.getType();
                if (event == null) {
                    LOG.info("event is null......continue");
                    continue;
                }

                Process process = ProcessManager.getInstance().getProcess(event);
                if (process == null) {
                    LOG.info("event is:{} without process", event.getVal());
                    continue;
                }
                LOG.info("{}=======process the msg:{}", process.toString(), event.getVal());
                process.excute(msg);

            } catch (Exception e) {
                LOG.error("Worker exception", e);
            }

        }
    }

}

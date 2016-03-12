package com.cube.event;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 异步响应记录对象
 * @description ReplyEvent
 * @author cgg
 * @version 0.1
 * @date 2014年8月16日
 */
public class ReplyEvent {

    private static final AtomicInteger ATOMIC_ID = new AtomicInteger(1);

    private Lock lock = new ReentrantLock();
    /**
     * 应答id
     */
    private final int id;
    /**
     * 应答信息
     */
    private volatile Object obj;

    public ReplyEvent() {
        this.id = ATOMIC_ID.getAndIncrement();
    }

    public Object getObj() {
        try {
            lock.lock();
            return obj;
        } finally {
            lock.unlock();
        }
    }

    public void setObj(Object obj) {
        try {
            lock.lock();
            this.obj = obj;
        } finally {
            lock.unlock();
        }
    }

    public int getId() {
        return id;
    }

}

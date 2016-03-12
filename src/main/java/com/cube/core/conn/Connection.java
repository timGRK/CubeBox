package com.cube.core.conn;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connection {

    /**
     * 链接id,链接的唯一标示
     */
    private final long id;
    /**
     * 链接的ChannelHandlerContext
     */
    private ChannelHandlerContext ctx;

    private volatile String mac;

    private boolean isSetMac = false;

    private Lock lock = new ReentrantLock();

    /**
     * 附带属性
     */
    private ConcurrentHashMap<String, Object> attr = new ConcurrentHashMap<String, Object>();

    public Object getAttr(String key) {
        return attr.get(key);
    }

    public Object putAttr(String key, Object value) {
        return attr.put(key, value);
    }

    public Object removeAttr(String key) {
        return attr.remove(key);
    }

    public Connection(long id, ChannelHandlerContext ctx) {
        this.id = id;
        this.ctx = ctx;
    }

    public long getId() {
        return id;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    /**
     * 线程安全,没有验验证通过前，返回null
     */
    public String getMac() {
        try {
            lock.lock();
            return mac;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 设置mac地址，只可以设置一次，线程安全
     */
    public boolean setMac(String mac) {
        try {
            lock.lock();
            if (isSetMac) {
                return false;
            }
            this.mac = mac;
            isSetMac = true;
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Connection other = (Connection) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return getClass().getName() + "@" + Integer.toHexString(super.hashCode()) + "->Connection [id=" + id
                + ", ctx=" + ctx + "]";
    }

}

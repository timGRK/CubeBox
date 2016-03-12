package com.cube.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cube.exception.NormalRuntimeException;

/**
 * zookeeper连接
 * @description CuratorBootstrap
 * @author cgg
 * @version 0.1
 * @date 2014年8月12日
 */
@Component
public class CuratorBootstrap implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(CuratorBootstrap.class);
    /**
     * 终结守护者
     */
    @SuppressWarnings("unused")
    private final Object finalizerGuardian = new Object() {

        @Override
        protected void finalize() throws Throwable {
            try {
                if (client != null) {
                    LOG.info("in finalize, close the curator client.^^^^^^^^^^^^^^^^");
                    CloseableUtils.closeQuietly(client);
                }
            } finally {
                super.finalize();
            }
        }

    };

    /**
     * zookeeper地址
     */
    @Value("${zk.hosts}")
    private String hosts;

    /**
     * zookeeper根目录
     */
    @Value("${zk.root}")
    private String root;

    /**
     * 本地tcp服务端口
     */
    @Value("${service.port}")
    private String servicePort;

    /**
     * 要保存在zk上的节点数据
     */
    private String peerData;
    /**
     * 本节点
     */
    private String znode;

    private CuratorFramework client;

    /**
     * 初始化，并连接服务端
     * @throws InterruptedException
     */
    private void initClient() throws InterruptedException {
        if (client == null) {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
            client = CuratorFrameworkFactory.newClient(hosts, retryPolicy);
        }
        if (client.getState() != CuratorFrameworkState.STARTED) {
            client.start();
            LOG.info("block until connected");
            client.blockUntilConnected();
        }
    }

    @Override
    public void run() {
        LOG.info("zk.hosts:{}", hosts);
        try {

            initClient();

            Stat stat = client.checkExists().forPath(root);
            if (stat == null) {
                root = client.create().forPath(root);
            }

            InetAddress ip = getAddress();
            peerData = "{0}:{1}";
            peerData = MessageFormat.format(peerData, ip.getHostAddress(), servicePort);

            znode =
                    client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                            .forPath(root + "/serve", peerData.getBytes());
            if(StringUtils.isBlank(znode)){
                LOG.error("znode 返回为空");
                throw new Exception("znode返回为空");
            }

        } catch (InterruptedException e) {
            LOG.info("curator is interrupt:", e);
            throw new NormalRuntimeException("在CuratorBootstrap的run中被打断", e);
        } catch (Exception e) {
            LOG.info("Exception:", e);
            throw new NormalRuntimeException("在CuratorBootstrap的run中异常", e);
        }

    }

    private InetAddress getAddress() {
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces
                    .hasMoreElements();) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                if (addresses.hasMoreElements()) {
                    return addresses.nextElement();
                }
            }
        } catch (SocketException e) {
            LOG.debug("Error when getting host ip address:", e);
            throw new NormalRuntimeException("获取ip出错", e);
        }
        return null;
    }

    public CuratorFramework getClient() {
        try {
            initClient();
        } catch (InterruptedException e) {
            LOG.info("initClient interrupt:", e);
            throw new NormalRuntimeException("initClient 打断异常", e);
        }
        return client;
    }

}

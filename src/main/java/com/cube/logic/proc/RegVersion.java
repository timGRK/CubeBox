package com.cube.logic.proc;

import io.netty.buffer.ByteBuf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.exception.IllegalDataException;
import com.cube.logic.Process;
import com.cube.utils.ByteBufUtils;
import com.cube.utils.CommUtils;
import com.cube.utils.MD5FileUtil;

/**
 * 注册版本号
 * @description
 * @author cgg
 * @version 0.1
 * @date 2014年8月6日
 */
public class RegVersion implements Process {

    private static final Logger LOG = LoggerFactory.getLogger(RegVersion.class);
    private static String LOCAL_WIFI_VER;
    private static Date LOCAL_WIFI_VER_SET_TIME;
    private static String LOCAL_MCU_VER;
    private static Date LOCAL_MCU_VER_SET_TIME;
    private static int expired = 5;// 5min

    private static byte[] wifiHead = new byte[] {(byte) 0xBB, (byte) 0x11, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static byte[] mcuHead = new byte[] {(byte) 0xBB, (byte) 0x22, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00};

    private static byte[] wifiMd5 = new byte[0];
    private static byte[] mcuMd5 = new byte[0];

    private static byte[] wifiFileContent = new byte[0];
    private static byte[] mcuFileContent = new byte[0];

    // private static Date wifi

    @Override
    public void excute(CubeMsg msg) throws IllegalDataException {
        byte[] verBit = msg.getData();
        if (verBit == null || (verBit.length != 2 && verBit.length != 3)) {
            LOG.info("版本注册数据错误,mac:{}", CommUtils.getMacFromAttr(msg.getCtx()));
        }
        // short ver = CommUtils.getVerFromByte(verBit);
        String ver = CommUtils.getVerFromByte(verBit);

       
        LOG.info("注册版本号mac:{}, ver:{}", CommUtils.getMacFromAttr(msg.getCtx()), ver);
        // msg.getCtx().channel().attr(SysConst.WIFI_VER_KEY).set(ver);
        if (ver.startsWith("00")) {
            LOG.info("验证wifi版本号");
            String loclWifiVer = getLocalWifiVer();
            if (ver.substring(2).equalsIgnoreCase(loclWifiVer)) {
                ByteBuf frame = ByteBufUtils.toFrameBuf(msg.getType().getVal(), new byte[] {0x00, 0x01});
                msg.getCtx().channel().writeAndFlush(frame);
            } else {
                ByteBuf frame = ByteBufUtils.toFrameBuf(msg.getType().getVal(), new byte[] {0x00, 0x00});
                msg.getCtx().channel().writeAndFlush(frame);
            }
        } else if (ver.startsWith("01")) {
            LOG.info("验证mcu版本号");
            String localMcuVer = getLocalMcuVer();
            if (ver.substring(2).equalsIgnoreCase(localMcuVer)) {
                ByteBuf frame = ByteBufUtils.toFrameBuf(msg.getType().getVal(), new byte[] {0x01, 0x01});
                msg.getCtx().channel().writeAndFlush(frame);
            } else {
                ByteBuf frame = ByteBufUtils.toFrameBuf(msg.getType().getVal(), new byte[] {0x01, 0x00});
                msg.getCtx().channel().writeAndFlush(frame);
            }
        } else {
            LOG.info("版本验证数据异常,关闭连接");
            msg.getCtx().channel().close();
        }
    }

    /**
     * 获取本地的wifi版本
     */
    private String getLocalWifiVer() {
        if (StringUtils.isBlank(LOCAL_WIFI_VER) || LOCAL_WIFI_VER_SET_TIME == null
                || expired(LOCAL_WIFI_VER_SET_TIME)) {
            LOCAL_WIFI_VER = readLocalWifiVer();
            if (StringUtils.isBlank(LOCAL_WIFI_VER)) {
                LOG.info("读取本地wifi版本为空");
                return "";
            }
            LOG.info("读取本地wifi版本：{}", LOCAL_WIFI_VER);
            LOCAL_WIFI_VER_SET_TIME = new Date();
        }
        return LOCAL_WIFI_VER;
    }

    /**
     * 获取本地mcu版本号
     */
    private String getLocalMcuVer() {
        if (StringUtils.isBlank(LOCAL_MCU_VER) || LOCAL_MCU_VER_SET_TIME == null || expired(LOCAL_MCU_VER_SET_TIME)) {
            LOCAL_MCU_VER = readLocalMcuVer();
            if (StringUtils.isBlank(LOCAL_MCU_VER)) {
                LOG.info("读取本地mcu版本为空");
                return "";
            }
            LOG.info("读取本地mcu版本：{}", LOCAL_MCU_VER);
            LOCAL_MCU_VER_SET_TIME = new Date();
        }
        return LOCAL_MCU_VER;
    }

    /**
     * 判断是否过期
     */
    private boolean expired(Date time) {
        long setTime = time.getTime();
        long now = System.currentTimeMillis();
        return (setTime + expired * 60 * 1000) < now;
    }

    /**
     * 从文件读取本地wifi版本号
     */
    private String readLocalWifiVer() {
        File dir = new File("/opt");
        File[] fm = dir.listFiles();
        File wifirom = null;
        for (File file : fm) {
            if (file.isDirectory()) {
                continue;
            }
            if (!file.getName().startsWith("wifirom.bin")) {
                continue;
            }
            wifirom = file;
            break;
        }
        if (wifirom != null) {
            String fName = wifirom.getName();
            String suffix = fName.substring(fName.lastIndexOf(".") + 1);
            if (suffix.length() == 4) {
                LOG.info("读取本地wifi版本号:{}", suffix);
                FileInputStream in = null;
                FileChannel ch = null;
                LOG.info("file length: {}", wifirom.length());
                byte[] fileContent = new byte[(int) wifirom.length()];
                LOG.info("fileContent length: {}", fileContent.length);
                try {
                    // 设置md5值
                    wifiMd5 = MD5FileUtil.getFileMD5Byte(wifirom);
                    in = new FileInputStream(wifirom);
                    int readlenth = in.read(fileContent);
                    LOG.info("read file length :{} ", readlenth);
                    // 读取wifi rom文件
                    wifiFileContent = fileContent;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    try {
                        if (ch != null) {
                            ch.close();
                        }
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return suffix;
            }
            LOG.info("读取wifi版本号错误:{}", suffix);
        }
        return null;
    }

    /**
     * 从文件读取本地mcu版本号
     */
    private String readLocalMcuVer() {
        File dir = new File("/opt");
        File[] fm = dir.listFiles();
        File mcurom = null;
        for (File file : fm) {
            if (file.isDirectory()) {
                continue;
            }
            if (!file.getName().startsWith("mcurom.bin")) {
                continue;
            }
            mcurom = file;
            break;
        }
        if (mcurom != null) {
            String fName = mcurom.getName();
            String suffix = fName.substring(fName.lastIndexOf(".") + 1);
            if (suffix.length() == 4) {
                LOG.info("读取本地mcu版本号:{}", suffix);

                FileInputStream in = null;
                FileChannel ch = null;
                LOG.info("file length: {}", mcurom.length());
                byte[] fileContent = new byte[(int) mcurom.length()];
                LOG.info("fileContent length: {}", fileContent.length);
                try {
                    // 设置md5值
                    mcuMd5 = MD5FileUtil.getFileMD5Byte(mcurom);
                    in = new FileInputStream(mcurom);
                    int readlenth = in.read(fileContent);
                    LOG.info("read file length :{} ", readlenth);
                    // 读取wifi rom文件
                    mcuFileContent = fileContent;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    try {
                        if (ch != null) {
                            ch.close();
                        }
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                return suffix;
            }
            LOG.info("读取mcu版本号错误:{}", suffix);
        }
        return null;
    }

    /**
     * 读取wifi的rom的md5值
     */
    private byte[] getWifiMd5() {
        return wifiMd5;
    }

    /**
     * 读取mcu的rom的md5值
     */
    private byte[] getMcuMd5() {
        return mcuMd5;
    }

    // wifiFileContent
}

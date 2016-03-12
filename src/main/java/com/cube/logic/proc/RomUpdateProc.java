package com.cube.logic.proc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.event.CubeMsg;
import com.cube.event.ReplyEvent;
import com.cube.exception.IllegalDataException;
import com.cube.logic.Process;
import com.cube.server.CubeBootstrap;
import com.cube.utils.CommUtils;
import com.cube.utils.MD5FileUtil;

/**
 * LED2Serve业务处理
 * @description HandShake
 * @author cgg
 * @version 0.1
 * @date 2014年8月6日
 */
public class RomUpdateProc implements Process {

    private static final Logger LOG = LoggerFactory.getLogger(RomUpdateProc.class);
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
    @Override
    public void excute(CubeMsg msg) throws IllegalDataException {
        LOG.info("收到LED回答:{}", new String(msg.getData()));
        byte[] verBit = msg.getData();
        if (verBit == null || verBit.length != 2 ) {
            LOG.info("升级请求数据错误,mac:{}", CommUtils.getMacFromAttr(msg.getCtx()));
            msg.getCtx().channel().close();
        }
        // short ver = CommUtils.getVerFromByte(verBit);
        String ver = CommUtils.getVerFromByte(verBit);

        if (ver.length() == 4 && ver.equals("0000")) {
            LOG.info("LED.wifi申请rom升级");
            // 如果没有加载rom文件，则会加载
            getLocalWifiVer();

            if (msg.getCtx().channel().isWritable()) {
                ByteBuf sendBytes = Unpooled.buffer();
                sendBytes.writeBytes(new byte[]{0x00, 0x07, (byte) 0xFF, (byte) 0xFF});
                sendBytes.writeBytes(wifiHead);
                sendBytes.writeBytes(wifiMd5);
                sendBytes.writeBytes(int2littleEndian(wifiFileContent.length));
//                sendBytes.writeInt(wifiFileContent.length);
                sendBytes.writeBytes(wifiFileContent);
                LOG.info("{}发送wifi OTA，mac:{}", msg.getCtx().channel().toString(),
                        CommUtils.getMacFromAttr(msg.getCtx()));
                msg.getCtx().channel().writeAndFlush(sendBytes);
            } else {
                LOG.info("channel不可写,关闭:{}", msg.getCtx().channel().toString());
                msg.getCtx().channel().close();
            }

            return;
        }
        if (ver.length() == 4 && ver.equals("0100")) {
            LOG.info("LED.mcu申请rom升级");
            // 如果没有加载rom文件，则会加载
            getLocalMcuVer();

            if (msg.getCtx().channel().isWritable()) {
                ByteBuf sendBytes = Unpooled.buffer();
                sendBytes.writeBytes(new byte[]{0x00, 0x07, (byte) 0xFF, (byte) 0xFF});
                sendBytes.writeBytes(mcuHead);
                sendBytes.writeBytes(mcuMd5);
                sendBytes.writeBytes(int2littleEndian(mcuFileContent.length));
//                sendBytes.writeInt(mcuFileContent.length);
                sendBytes.writeBytes(mcuFileContent);
                LOG.info("{}发送mcu，mac:{}", msg.getCtx().channel().toString(), CommUtils.getMacFromAttr(msg.getCtx()));
                msg.getCtx().channel().writeAndFlush(sendBytes);
            } else {
                LOG.info("channel不可写,关闭:{}", msg.getCtx().channel().toString());
                msg.getCtx().channel().close();
            }

            return;
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
    
    private byte[] int2littleEndian(int n){
    	byte[] bits = new byte[4];
    	bits[0] = (byte) (n & 0x000000FF);
    	bits[1] = (byte) ((n >> 8) & 0x000000FF);
    	bits[2] = (byte) ((n >> 16) & 0x000000FF);
    	bits[3] = (byte) ((n >> 24) & 0x000000FF);
    	return bits;
    }

}

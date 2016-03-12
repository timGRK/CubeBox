package com.cube.logic.http;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.Attribute;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.core.common.SysConst;
import com.cube.core.conn.Connection;
import com.cube.core.conn.ConnectionManager;
import com.cube.logic.DefaultHttpProcess;
import com.cube.logic.proc.OTAReplyProc;
import com.cube.utils.MD5FileUtil;

/**
 * OTA升级
 * @description OtaProc
 * @author cgg
 * @version 0.1
 * @date 2014年8月16日
 */
public class OtaProc extends DefaultHttpProcess {

    private static final Logger LOG = LoggerFactory.getLogger(OtaProc.class);
    private static Lock OTA_LOCK = new ReentrantLock();
    public byte[] head = new byte[] {(byte) 0xBB, (byte) 0x11, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00};

    @Override
    protected void doExcute(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp,
            Map<String, List<String>> params, HttpPostRequestDecoder decoder) throws IOException {
        try {
            LOG.info("OTA升级");
            String checksum = null;
            String ver = null;
            if (method(req) == HttpMethod.GET) {
                checksum = getParam("checksum", params);
                ver = getParam("ver", params);
                LOG.info("method GET 获得checksum");
                LOG.info("method GET 获得ver");
            } else {
                InterfaceHttpData checksumData = getHttpData("checksum", decoder);
                InterfaceHttpData verData = getHttpData("ver", decoder);
                checksum = parseData(checksumData);
                ver = parseData(verData);
                LOG.info("method POST 获得checksum");
                LOG.info("method POST 获得ver");
            }
            if (StringUtils.isBlank(checksum)) {
                LOG.info("获取的checksum为空");
                sendParamsError("checksum不能为空", resp);
                return;
            }
            if (StringUtils.isBlank(ver)) {
                LOG.info("获取的ver为空");
                sendParamsError("ver不能为空", resp);
                return;
            }
            
            int iVer = Integer.valueOf(ver, 16);
            String rompath = null;
            if((iVer & 0x4000) == 0x4000){
            	rompath = System.getProperty("rompathMcu", "");
            }else{
            	rompath = System.getProperty("rompath", "");
            }
            
            if (StringUtils.isBlank(rompath)) {
                LOG.info("获取的rompath为空");
                sendParamsError("rompath不能为空", resp);
                return;
            }

            LOG.info("checksum是:{},ver是:{}, rompath:{}", new Object[] {checksum, ver, rompath});

            File file = new File(rompath);
            if (!file.isFile()) {
                sendParamsError("rompath:" + rompath + "，找不到该文件", resp);
                return;
            }
            // String md5 = MD5FileUtil.getFileMD5String(file);
            byte[] md5byte = MD5FileUtil.getFileMD5Byte(file);
            String md5 = MD5FileUtil.bufferToHex(md5byte);
            if (!checksum.equalsIgnoreCase(md5)) {
                sendParamsError("checksum不一样:" + md5, resp);
                return;
            }
            FileInputStream in = null;
            FileChannel ch = null;
            LOG.info("file length: {}", file.length());
            byte[] fileContent = new byte[(int) file.length()];
            LOG.info("fileContent length: {}", fileContent.length);
            try {

                in = new FileInputStream(file);
                int readlenth = in.read(fileContent);
                LOG.info("read file length :{} ", readlenth);
            } finally {
                if (ch != null) {
                    ch.close();
                }
                if (in != null) {
                    in.close();
                }
            }
            Enumeration<String> keys = ConnectionManager.getInstance().keys();
            try {
                OTA_LOCK.lock();
                OTAReplyProc.SUCCESS.set(0);
                OTAReplyProc.TOTAL.set(0);

                while (keys.hasMoreElements()) {
                    String mac = keys.nextElement();
                    Connection conn = ConnectionManager.getInstance().getConn(mac);
                    if (conn == null) {
                        sendUnableConn(resp);
                        continue;
                    }
                    Attribute<String> verAttr = conn.getCtx().channel().attr(SysConst.WIFI_VER_KEY);
                    String shVer = verAttr.get();
                    if (shVer == null) {
                        LOG.info("版本未注册mac:{}", mac);
                        continue;
                    }
                    if (!ver.equalsIgnoreCase(shVer)) {
                        LOG.info("{},版本错误mac:{}, ver:{}", new Object[] {conn.getCtx().channel().toString(), mac,
                                shVer});
                        continue;
                    }
                    /*
                     * if(((shVer & 0xff00) ^ ((v[0] << 8) & 0xff00)) != 0){
                     * LOG.info("版本错误:mac{}, ver:{}", mac, shVer); continue; }
                     * if(((shVer & 0x00ff) ^ v[1]) != 0){
                     * LOG.info("版本错误:mac{}, ver:{}", mac, shVer); continue; }
                     */
                    if (conn.getCtx().channel().isWritable()) {
                        ByteBuf sendBytes = Unpooled.buffer();
                        sendBytes.writeBytes(head);
                        sendBytes.writeBytes(md5byte);
                        sendBytes.writeInt(fileContent.length);
                        sendBytes.writeBytes(fileContent);
                        LOG.info("{}发送OTA，mac:{}", conn.getCtx().channel().toString(), mac);
                        OTAReplyProc.TOTAL.incrementAndGet();
                        conn.getCtx().channel().writeAndFlush(sendBytes);
                    }
                }

            } finally {
                resp.content().writeBytes(("OTA升级，总共升级" + OTAReplyProc.TOTAL.get() + "个LED").getBytes());
                resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
                OTA_LOCK.unlock();
            }

        } finally {
        }

    }

}

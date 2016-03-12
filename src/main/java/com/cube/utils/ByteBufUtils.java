package com.cube.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import org.apache.commons.lang.StringUtils;

public class ByteBufUtils {

    /**
     * 按ascii转换
     */
    public static ByteBuf str2Buf(String str) {
        if (StringUtils.isBlank(str)) {
            return Unpooled.EMPTY_BUFFER;
        }
        return Unpooled.copiedBuffer(str, CharsetUtil.US_ASCII);
    }
    /**
     * 按ascii转换为传输帧, 需要自己release 参数中的content
     */
    public static ByteBuf toFrameBuf(byte[] event,ByteBuf content){
        Integer length = content.readableBytes();
        ByteBuf frame = Unpooled.buffer(4 + length);
        frame.writeBytes(event);
        frame.writeShort(length.shortValue());
        frame.writeBytes(content);
        return frame;
    }
    
    /**
     * 按ByteBuf转换为传输帧,需要自己release 参数中的content
     */
    public static ByteBuf toFrameBuf(short event,ByteBuf content){
        Integer length = content.readableBytes();
        ByteBuf frame = Unpooled.buffer(4 + length);
        frame.writeShort(event);
        frame.writeShort(length.shortValue());
        frame.writeBytes(content);
        return frame;
    }
    /**
     * 按byte[]转换为传输帧
     */
    public static ByteBuf toFrameBuf(short event, byte[] bits){
        int length = 0;
        if(bits != null && bits.length != 0){
            length = bits.length;
        }
        ByteBuf frame = Unpooled.buffer(4 + length);
        frame.writeShort(event);
        frame.writeShort(length);
        frame.writeBytes(bits);
        return frame;
    }
    
    /**
     * 通过验证
     */
    private static final byte[] passSec = new byte[]{0x00, 0x01, 0x00, 0x01, 0x01};
    /**
     * 没有通过验证
     */
    private static final byte[] unpassSec = new byte[]{0x00, 0x01, 0x00, 0x01, 0x00};
    /**
     * 心跳
     */
    private static final byte[] heartbeat = new byte[]{0x00, 0x05, 0x00, 0x00};
    
    public static ByteBuf passSecure(){
    	ByteBuf frame = Unpooled.copiedBuffer(passSec);
    	return frame;
    }
    public static ByteBuf unpassSecure(){
    	ByteBuf frame = Unpooled.copiedBuffer(unpassSec);
    	return frame;
    }
    /**
     * 获取心跳包
     */
    public static ByteBuf headBeat(){
        return Unpooled.copiedBuffer(heartbeat);
    }
}

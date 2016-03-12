package com.cube.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureUtils {

	private static final Logger LOG  = LoggerFactory.getLogger(SecureUtils.class);
    /*
     * checksum= F(key) string F(string key){ string mac = getMac(); string salt
     * = getSalt(key, 0, 0); string sum = mac + salt;//字符串拼接 string md5 =
     * md5(sum); return md5; } string getSalt(string key, int n, int length){
     * string curChar = key[n]; length++; if(length == 8){ return curChar; }
     * return curChar + getSalt(key, (int)curChar, length); }
     */
    public static String encode(String key, String mac)  {
    	LOG.info("key:{}, mac:{}", key, mac);
        String salt = getSalt(key, 0, 0);
        String sum = mac + salt;
        
            try {
                MessageDigest msgDigest = MessageDigest.getInstance("MD5");
                byte[] checksum = msgDigest.digest(sum.getBytes());
                StringBuilder sb = new StringBuilder();
                for(byte b : checksum){
                    int k = b;
                    k = 0x00ff & k;
                    String hex = Integer.toHexString(k);
                    if(hex.length()==1){
                    	sb.append("0");
                    }
                    sb.append(hex);
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                LOG.error("key加密出错", e);
                return "";
            }
       
    }

    public static String getSalt(String key, int n, int length) {
        n = n % key.length();
        String curChar = "" + key.charAt(n);
        length++;
        if (length == 8) {
            return curChar;
        }
        return curChar + getSalt(key, (int) key.charAt(n), length);
    }
}

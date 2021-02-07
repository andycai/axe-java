package com.iwayee.activity.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptUtil {
  public static String getMD5Str(String str) {
    byte[] digest = null;
    try {
      MessageDigest md5 = MessageDigest.getInstance("md5");
      digest  = md5.digest(str.getBytes("utf-8"));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    //16是表示转换为16进制数
    String md5Str = new BigInteger(1, digest).toString(16);
    return md5Str;
  }

  public static String md5(String s) {
    String ret = "";
    try {
      // 通过调用MessageDigest（数据摘要类）的getInstance()静态方法，传入加密算法的名称，获取数据摘要对象。
      //MessageDigest MessageDigest.getInstance(algorithm);
      MessageDigest messageDigest = MessageDigest.getInstance("MD5");
      // 获取摘要（加密），结果是字节数组
      // byte[] java.security.MessageDigest.digest(byte[] input)
      byte[] ciphertext = messageDigest.digest(s.getBytes());

      // 利用apache的commons-codec，将字节数组转换为十六进制。
//      ret = Hex.encodeHexString(ciphertext);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return ret;
  }

  public static String sha1(String s) {
    String ret = "";
    try {
      // 获取指定摘要算法的messageDigest对象
      MessageDigest messageDigest = MessageDigest.getInstance("SHA"); // 此处的sha代表sha1
      // 调用digest方法，进行加密操作
      byte[] cipherBytes = messageDigest.digest(s.getBytes());

//      ret = Hex.encodeHexString(cipherBytes);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return "";
  }
}

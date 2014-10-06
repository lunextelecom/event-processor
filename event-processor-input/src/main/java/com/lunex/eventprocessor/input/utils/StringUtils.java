package com.lunex.eventprocessor.input.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class process String
 *
 */
public class StringUtils {

  /**
   * Check json string is valid
   * 
   * @param test
   * @return
   */
  public static boolean isJSONValid(String test) {
    try {
      new JSONObject(test);
    } catch (JSONException ex) {
      // edited, to include @Arthur's comment
      // e.g. in case JSONArray is valid as well...
      try {
        new JSONArray(test);
      } catch (JSONException ex1) {
        return false;
      }
    }
    return true;
  }

  /**
   * Create md5 string
   *
   * @param message
   * @return
   */
  public static String md5Java(String message) {
    String digest = null;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hash = md.digest(message.getBytes("UTF-8"));
      // converting byte array to Hexadecimal String
      StringBuilder sb = new StringBuilder(2 * hash.length);
      for (byte b : hash) {
        sb.append(String.format("%02x", b & 0xff));
      }
      digest = sb.toString();
    } catch (UnsupportedEncodingException ex) {
    } catch (NoSuchAlgorithmException ex) {
    }
    return digest;
  }
}

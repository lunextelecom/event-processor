package com.lunex.eventprocessor.core.utils;

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
   * @throws UnsupportedEncodingException
   * @throws NoSuchAlgorithmException
   */
  public static String md5Java(String message) throws UnsupportedEncodingException,
      NoSuchAlgorithmException {
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
      throw ex;
    } catch (NoSuchAlgorithmException ex) {
      throw ex;
    }
    return digest;
  }

  public enum BackFillEnum {
    day, month, year, hour, minute, second
  }

  // # N day, n month, n year, n hour, n minute, n second
  public static long getBackFillTime(String backfillTime) {
    try {
      String[] temp = backfillTime.split(" ");
      long time = 1000L;
      switch (BackFillEnum.valueOf(temp[1])) {
        case second:
          time *= Integer.valueOf(temp[0]);
          break;
        case minute:
          time *= Integer.valueOf(temp[0]) * 60;
          break;
        case hour:
          time *= Integer.valueOf(temp[0]) * 60 * 60;
          break;
        case day:
          time *= Integer.valueOf(temp[0]) * 60 * 60 * 24;
          break;
        case month:
          time *= Integer.valueOf(temp[0]) * 60 * 60 * 24 * 30;
          break;
        case year:
          time *= Integer.valueOf(temp[0]) * 60 * 60 * 24 * 30 * 12;
          break;
        default:
          return -1;
      }
      return System.currentTimeMillis() - time;
    } catch (Exception ex) {
      return -1;
    }
  }
}

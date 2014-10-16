package com.lunex.eventprocessor.core.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class process String
 *
 */
public class StringUtils {

  public static String randomString(int num) {
    char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    StringBuilder sb = new StringBuilder();
    Random random = new Random();
    for (int i = 0; i < num; i++) {
        char c = chars[random.nextInt(chars.length)];
        sb.append(c);
    }
    String output = sb.toString();
    return output;
  }
  
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

  /**
   * Convert sum(amount), count(txId) -> sum(amount) as sum_amount, count(txId) as count_txId
   * 
   * @param field
   * @return
   */
  public static String convertField(String field) {
    String[] fields = field.split(",");
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < fields.length; i++) {
      String temp = fields[i].replaceAll("[(]+", "_");
      temp = temp.replace(")", "");
      builder.append(fields[i] + " as " + temp);
      if (i < fields.length - 1)
        builder.append(", ");
    }
    return builder.toString();
  }

  /**
   * Convert sum(amount), count(txId) -> sum(sum_amount), sum(count_txId)
   * 
   * @param field
   * @return
   */
  public static String convertField2(String field) {
    String[] fields = field.split(",");
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < fields.length; i++) {
      String temp = fields[i].trim().replaceAll("[(]+", "_");
      temp = temp.replace(")", "");
      if (fields[i].contains("count")) {
        fields[i] = fields[i].replace("count", "sum");
      }
      builder.append(fields[i].replaceAll("\\([a-zA-Z]+\\)", "(" + temp + ")") + " as " + temp);
      if (i < fields.length - 1)
        builder.append(", ");
    }
    return builder.toString();
  }

  /**
   * Revert sum_amount-> sum(amount) or count_txId -> count(txid)
   * 
   * @param field
   * @return
   */
  public static String revertSingleField(String field) {
    return field.replace("_", "(") + ")";
  }

  /**
   * Create crontab for every timeframe
   * 
   * @param smallBucket
   */
  public static List<String> convertCrontab(String smallBucket) {
    List<String> result = new ArrayList<String>();
    if (smallBucket.contains(":truncate")) {
      String startTime = "";
      String endTime = "";
      smallBucket = smallBucket.replaceAll(" +s", " ");
      String temp[] = smallBucket.split(":");
      if (temp.length > 1) {
        smallBucket = temp[0];
        temp = smallBucket.split(" ");
        if (temp.length > 1) {
          String time = temp[0];
          String label = temp[1];
          switch (label) {
            case "minute":
            case "minutes":
              startTime = "*/" + time + ", *, *, *, *";
              endTime = "*/" + time + ", *, *, * , *, 0";
              break;
            case "hours":
            case "hour":
              startTime = "*, */" + time + ", *, *, *";
              endTime = "0, */" + time + ", *, * , *, 0";
              break;
            case "days":
            case "day":
              startTime = "*, *, */" + time + ", *, *";
              endTime = "0, 0, */" + time + ", * , *, 0";
              break;
            case "week":
            case "weeks":
              startTime = "*, *, *, *, MON/" + time + "";
              endTime = "0, 0, *, * , MON/" + time + ", 0";
              break;
            default:
              break;
          }
        }
      }
      result.add(startTime);
      result.add(endTime);
      return result;
    } else {
      result.add(smallBucket);
      return result;
    }
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

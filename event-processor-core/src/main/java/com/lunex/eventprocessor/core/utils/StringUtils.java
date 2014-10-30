package com.lunex.eventprocessor.core.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

  public static boolean checkFieldFunction(String field) {
    if (field.startsWith("sum(") || field.startsWith("min(") || field.startsWith("max(")
        || field.startsWith("avg(") || field.startsWith("first(") || field.startsWith("last(") || field.startsWith("count(")) {
      return true;
    }
    return false;
  }

  public final static String seperatorField = "___";

  /**
   * Convert sum(amount), count(txId) -> sum(amount) as sum___amount, count(txId) as count___txId
   * 
   * @param field
   * @return
   */
  public static String convertField(String field) {
    String[] fields = field.split(",");
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < fields.length; i++) {
      if (checkFieldFunction(fields[i].trim())) {
        String temp = fields[i].replaceAll("[(]+", seperatorField);
        temp = temp.replace(")", Constants.EMPTY_STRING);
        builder.append(fields[i] + " as " + temp);
      } else {
        builder.append(fields[i] + " as " + fields[i]);
      }
      if (i < fields.length - 1)
        builder.append(", ");
    }
    return builder.toString();
  }

  /**
   * Convert sum(amount), count(txId) -> sum(sum___amount), sum(count___txId)
   * 
   * @param field
   * @return
   */
  public static String convertField2(String field) {
    String[] fields = field.split(",");
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < fields.length; i++) {
      if (checkFieldFunction(fields[i].trim())) {
        String temp = fields[i].trim().replaceAll("[(]+", seperatorField);
        temp = temp.replace(")", Constants.EMPTY_STRING);
        if (fields[i].contains("count")) {
          fields[i] = fields[i].replace("count", "sum");
        }
        builder.append(fields[i].replaceAll("\\([a-zA-Z]+\\)", "(" + temp + ")") + " as " + temp);
      } else {
        builder.append(fields[i] + " as " + fields[i]);
      }
      if (i < fields.length - 1)
        builder.append(", ");
    }
    return builder.toString();
  }

  /**
   * Revert sum___amount-> sum(amount) or count___txId -> count(txid)
   * 
   * @param field
   * @return
   */
  public static String revertSingleField(String field) {
    if (field.contains(seperatorField))
      return field.replace(seperatorField, "(") + ")";
    return field;
  }

  public static Map<String, Object> revertHashMapField(Map<String, Object> map) {
    Iterator<String> it = map.keySet().iterator();
    Map<String, Object> result = new HashMap<String, Object>();
    while (it.hasNext()) {
      String key = it.next();
      Object obj = map.get(key);
      result.put(revertSingleField(key), obj);
    }
    return result;
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
              startTime = "0, */" + time + ", *, *, *";
              endTime = "0, */" + time + ", *, * , *, 0";
              break;
            case "days":
            case "day":
              startTime = "0, 0, */" + time + ", *, *";
              endTime = "0, 0, */" + time + ", * , *, 0";
              break;
            case "week":
            case "weeks":
              startTime = "*, *, *, *, */" + time + "";
              endTime = "0, 0, *, * , 1/" + time + ", 0";
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

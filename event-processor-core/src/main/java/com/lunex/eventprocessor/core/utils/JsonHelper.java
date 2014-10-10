package com.lunex.eventprocessor.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonHelper {

  /**
   * Convert Object to Json
   */
  public static Object toJSON(Object object) throws JSONException {
    if (object instanceof Map) {
      JSONObject json = new JSONObject();
      Map<String, Object> map = (Map<String, Object>) object;
      for (Object key : map.keySet()) {
        json.put(key.toString(), toJSON(map.get(key)));
      }
      return json;
    } else if (object instanceof Iterable) {
      JSONArray json = new JSONArray();
      for (Object value : ((Iterable<?>) object)) {
        json.put(value);
      }
      return json;
    } else {
      return object;
    }
  }

  /**
   * Check object is empty
   */
  public static boolean isEmptyObject(JSONObject object) {
    return object.names() == null;
  }

  /**
   * Get Map from JsonObject with key
   */
  public static Map<String, Object> getMap(JSONObject object, String key) throws JSONException {
    return toMap(object.getJSONObject(key));
  }

  /**
   * Convert JsonObject to map
   */
  public static Map<String, Object> toMap(JSONObject object) throws JSONException {
    Map<String, Object> map = new HashMap<String, Object>();
    Iterator<?> keys = object.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      map.put(key, fromJson(object.get(key)));
    }
    return map;
  }

  public static List<Object> toList(JSONArray array) throws JSONException {
    List<Object> list = new ArrayList<Object>();
    for (int i = 0; i < array.length(); i++) {
      list.add(fromJson(array.get(i)));
    }
    return list;
  }

  /**
   * Get Object from Json
   */
  private static Object fromJson(Object json) throws JSONException {
    if (json == JSONObject.NULL) {
      return null;
    } else if (json instanceof JSONObject) {
      return toMap((JSONObject) json);
    } else if (json instanceof JSONArray) {
      return toList((JSONArray) json);
    } else {
      return json;
    }
  }

  /**
   * Convert Map to JSonString
   * 
   * @param map
   * @return
   * @throws Exception
   */
  public static String toJSonString(Map<String, Object> map) throws Exception {
    try {
      JSONObject json = new JSONObject(map);
      return json.toString();
    } catch (Exception ex) {
      throw ex;
    }
  }

  /**
   * Convert Map to JsonObject
   * 
   * @param map
   * @return
   * @throws Exception
   */
  public static JSONObject toJSonObject(Map<String, Object> map) throws Exception {
    try {
      return new JSONObject(map);
    } catch (Exception ex) {
      throw ex;
    }
  }
}

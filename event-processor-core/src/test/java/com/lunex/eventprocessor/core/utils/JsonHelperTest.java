package com.lunex.eventprocessor.core.utils;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonHelperTest {

  @Test
  public void testToJSON() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("eventName", "new_order");
    JSONObject json = (JSONObject) JsonHelper.toJSON(map);
    assertEquals(json.get("eventName"), map.get("eventName"));
  }

  @Test
  public void testToMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("eventName", "new_order");
    JSONObject json = (JSONObject) JsonHelper.toJSON(map);
    Map<String, Object> newmap = JsonHelper.toMap(json);
    assertEquals(newmap.get("eventName"), map.get("eventName"));
  }

  @Test
  public void testToJSonString() throws Exception {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("eventName", "new_order");
    String result = JsonHelper.toJSonString(map);
    assertEquals(result, "{\"eventName\":\"new_order\"}");
  }

  @Test
  public void testToJSonObject() throws Exception {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("eventName", "new_order");
    JSONObject json = JsonHelper.toJSonObject(map);
    assertEquals(json.get("eventName"), map.get("eventName"));
  }

}

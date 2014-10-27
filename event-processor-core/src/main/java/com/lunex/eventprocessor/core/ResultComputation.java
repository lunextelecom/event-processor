package com.lunex.eventprocessor.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.lunex.eventprocessor.core.utils.JsonHelper;

public class ResultComputation implements Serializable {

  private static final long serialVersionUID = 8813738595877560011L;

  private long time = System.currentTimeMillis();
  private String eventName;
  private String ruleName;
  private Map<String, Object> result = new HashMap<String, Object>();
  // private String payLoadStr;
  private String hashKey;

  public ResultComputation(String eventName, String ruleName, long time, String hashKey,
      String result) {
    try {
      JSONObject jsonObject = new JSONObject(result);
      this.result = JsonHelper.toMap(jsonObject);
      this.eventName = eventName;
      this.ruleName = ruleName;
      this.time = time;
      this.hashKey = hashKey;
    } catch (Exception ex) {
      this.eventName = null;
      this.ruleName = null;
      this.result = null;
      this.time = -1;
    }
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public String getRuleName() {
    return ruleName;
  }

  public void setRuleName(String ruleName) {
    this.ruleName = ruleName;
  }

  public Map<String, Object> getResult() {
    return result;
  }

  public void setResult(Map<String, Object> result) {
    this.result = result;
  }

  // public String getPayLoadStr() {
  // return payLoadStr;
  // }
  // public void setPayLoadStr(String payLoadStr) {
  // this.payLoadStr = payLoadStr;
  // }
  public String getHashKey() {
    return hashKey;
  }

  public void setHashKey(String hashKey) {
    this.hashKey = hashKey;
  }

}

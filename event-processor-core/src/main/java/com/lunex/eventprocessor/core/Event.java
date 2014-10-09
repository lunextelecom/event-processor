package com.lunex.eventprocessor.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.lunex.eventprocessor.core.utils.JsonHelper;

public class Event implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -3901685029349769059L;

  private long time = System.currentTimeMillis();
  private String evtName;
  private Map<String, Object> event = new HashMap<String, Object>();
  private String payLoadStr;

  public Event() {}

  public Event(long time, String evtName, Map<String, Object> event) {
    this.time = time;
    this.evtName = evtName;
    this.event = event;
  }

  public Event(String evtName, Map<String, Object> event) {
    this.evtName = evtName;
    this.event = event;
    this.time = (Long) event.get("time");
  }

  public Event(long time, String payLoad) {
    try {
      JSONObject jsonObject = new JSONObject(payLoad);
      this.time = time;
      this.evtName = jsonObject.getString("evtName");
      this.event = JsonHelper.toMap(jsonObject);
      this.payLoadStr = payLoad;
    } catch (Exception ex) {
      this.evtName = null;
      this.event = null;
      this.time = -1;
    }
  }

  public Event(String payLoad) {
    try {
      JSONObject jsonObject = new JSONObject(payLoad);
      this.time = jsonObject.getLong("time");;
      this.evtName = jsonObject.getString("evtName");
      this.event = JsonHelper.toMap(jsonObject);
      this.payLoadStr = payLoad;
    } catch (Exception ex) {
      this.evtName = null;
      this.event = null;
      this.time = -1;
    }
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public String getEvtName() {
    return evtName;
  }

  public void setEvtName(String evtName) {
    this.evtName = evtName;
  }

  public Map<String, Object> getEvent() {
    return event;
  }

  public void setEvent(Map<String, Object> event) {
    this.event = event;
  }

  @Override
  public String toString() {
    return "time: " + this.time + ", name: " + this.evtName + ", event: " + this.event.toString();
  }

  public String getPayLoadStr() {
    return payLoadStr;
  }

  public void setPayLoadStr(String payLoadStr) {
    this.payLoadStr = payLoadStr;
  }
}

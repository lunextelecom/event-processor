package com.lunex.eventprocessor.core.bean;

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
  private String name;
  private Map<String, Object> event = new HashMap<String, Object>();

  public Event() {}

  public Event(String evtName, Map<String, Object> event) {
    this.name = evtName;
    this.event = event;
  }

  public Event(String jsonStr) {
    try {
      JSONObject jsonObject = new JSONObject(jsonStr);
      this.name = jsonObject.getString("evtName");
      this.event = JsonHelper.toMap(jsonObject);
    } catch (Exception ex) {
      this.name = null;
      this.event = null;
    }
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, Object> getEvent() {
    return event;
  }

  public void setEvent(Map<String, Object> event) {
    this.event = event;
  }
}

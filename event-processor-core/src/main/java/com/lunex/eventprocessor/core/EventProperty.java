package com.lunex.eventprocessor.core;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.lunex.eventprocessor.core.utils.JsonHelper;

/**
 * This is just a class used to represent mapping type data of properties in event Ex:
 * evtName:new-order, properties: {"amount":"double", "reseller":"string", "sourceIp":"string"}
 * 
 */
public class EventProperty {

  private String evtDataName;
  private Map<String, Object> properties = new HashMap<String, Object>();

  public EventProperty(String evtName, String jsonStr) {
    try {
      this.evtDataName = evtName;
      if (jsonStr != null) {
        JSONObject jsonObject = new JSONObject(jsonStr);
        this.properties = JsonHelper.toMap(jsonObject);
      }
    } catch (Exception ex) {
      this.evtDataName = null;
      this.properties = null;
    }
  }

  public String getEvtDataName() {
    return evtDataName;
  }

  public void setEvtDataName(String evtDataName) {
    this.evtDataName = evtDataName;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

}

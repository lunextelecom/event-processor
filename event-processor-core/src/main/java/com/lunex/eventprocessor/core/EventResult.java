package com.lunex.eventprocessor.core;

import java.io.Serializable;

public class EventResult implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 8950786068549977659L;

  private String eventName;
  private String hashKey;
  private String result;
  private String filteredResult;

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public String getHashKey() {
    return hashKey;
  }

  public void setHashKey(String hashKey) {
    this.hashKey = hashKey;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getFilteredResult() {
    return filteredResult;
  }

  public void setFilteredResult(String filteredResult) {
    this.filteredResult = filteredResult;
  }

  public EventResult() {

  }

  public EventResult(String eventName, String hashkey, String result, String filteredResult) {
    this.eventName = eventName;
    this.hashKey = hashkey;
    this.result = result;
    this.filteredResult = filteredResult;
  }

}

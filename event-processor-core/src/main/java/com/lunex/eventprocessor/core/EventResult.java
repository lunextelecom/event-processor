package com.lunex.eventprocessor.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EventResult implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 8950786068549977659L;

  private String eventName;
  private String hashKey;
  private List<String> result;
  private List<String> filteredResult;

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

  public List<String> getResult() {
    return result;
  }

  public void setResult(List<String> result) {
    this.result = result;
  }

  public List<String> getFilteredResult() {
    return filteredResult;
  }

  public void setFilteredResult(List<String> filteredResult) {
    this.filteredResult = filteredResult;
  }

  public EventResult() {

  }

  public EventResult(String eventName, String hashkey, String result, String filteredResult) {
    this.eventName = eventName;
    this.hashKey = hashkey;
    List<String> results = new ArrayList<String>();
    if (result != null)
      results.add(result);
    this.result = results;
    List<String> filteredResults = new ArrayList<String>();
    if (filteredResult != null)
      filteredResults.add(filteredResult);
    this.filteredResult = filteredResults;
  }

}

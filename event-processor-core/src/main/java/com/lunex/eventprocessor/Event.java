package com.lunex.eventprocessor;

import java.io.Serializable;
import java.util.Map;

/**
 * Generic Event.
 *
 */
public class Event implements Serializable {

  private long time;
  private String name;
  private Map<String, Object> event;

  public Event(long time, String name, Map<String, Object> event) {
    this.time = time;
    this.name = name;
    this.event = event;
  }

  public long getTime() {
    return time;
  }

  public String getName() {
    return name;
  }

  public Map<String, Object> getEvent() {
    return event;
  }

}




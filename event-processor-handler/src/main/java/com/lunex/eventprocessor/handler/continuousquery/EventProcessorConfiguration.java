package com.lunex.eventprocessor.handler.continuousquery;

import java.util.HashMap;
import java.util.Map;

import com.espertech.esper.client.Configuration;

/**
 * Configuration for EPL service
 *
 */
public class EventProcessorConfiguration {

  private Configuration config;
  private Map<String, String> eventClasses;

  public Configuration getConfig() {
    return config;
  }

  public void setConfig(Configuration config) {
    this.config = config;
  }

  public Map<String, String> getEventClasses() {
    return eventClasses;
  }

  public void setEventClasses(Map<String, String> eventClasses) {
    this.eventClasses = eventClasses;
  }

  public EventProcessorConfiguration() {
    eventClasses = new HashMap<String, String>();
    config = new Configuration();
  }

  public void addEventType(String eventTypeName, String eventClassName) {
    this.eventClasses.put(eventTypeName, eventClassName);
    config.addEventType(eventTypeName, eventClassName);
  }

  public void addEventType(Class<?> eventClass) {
    addEventType(eventClass.getSimpleName(), eventClass.getName());
    config.addEventType(eventClass);
  }

  public void addEventType(String eventTypeName, Class<?> eventClass) {
    addEventType(eventTypeName, eventClass.getName());
    config.addEventType(eventTypeName, eventClass);
  }
}

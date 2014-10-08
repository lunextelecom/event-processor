package com.lunex.eventprocessor.core;

/**
 * This is just a class used to represent the content of the Rules that is read from datastore. It
 * is not the actually esper query. It’s the pieces that we used to build the esper query
 */
public class EventQuery {

  private String eventName;
  private String data;
  private String fields;
  private String filters;
  private String aggregateField;
  private String timeSeries;

  QueryFuture getFuture() {
    return null;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getFields() {
    return fields;
  }

  public void setFields(String fields) {
    this.fields = fields;
  }

  public String getFilters() {
    return filters;
  }

  public void setFilters(String filters) {
    this.filters = filters;
  }

  public String getAggregateField() {
    return aggregateField;
  }

  public void setAggregateField(String aggregateField) {
    this.aggregateField = aggregateField;
  }

  public String getTimeSeries() {
    return timeSeries;
  }

  public void setTimeSeries(String timeSeries) {
    this.timeSeries = timeSeries;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }
}

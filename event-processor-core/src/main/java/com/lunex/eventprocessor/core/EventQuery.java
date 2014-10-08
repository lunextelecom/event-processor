package com.lunex.eventprocessor.core;

/**
* SELECT [Fields] FROM [Data] WHERE [Filter] GROUP BY [AggregateField]
* Query Parts
* Data: raw incoming data or generated time series
* AggregateField: Fields that are used to build aggregation of data.
* Timeseries is a special function here to group data into timeseries.
* Filter: conditions to filter data. =, !=, >=, <=, >, <, and, or
* Field: field1, field2, or * for all
* Field func: sum, max, min, first, last, avg, timeseries(timefield, size1, size2[optional])
*/
public class EventQuery {

  private String eventName;
  private String data;
  private String fields;
  private String filters;
  private String aggregateField;
  private String having;
  private String timeSeries;

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

  public String getHaving() {
    return having;
  }

  public void setHaving(String having) {
    this.having = having;
  }
}

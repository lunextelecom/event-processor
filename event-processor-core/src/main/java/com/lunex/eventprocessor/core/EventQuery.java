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
  private String ruleName;
  private String data;
  // Ex: from new_order
  private String fields;
  // Ex: select sum(amount:double), count(txId:long), seller:string
  private String filters;
  // Ex: where seller = 'AAA'
  private String aggregateField;
  // Ex: group by seller
  private String having;
  // Ex: esper select sum(amount:double), count(txId:long), seller:string from new_order.win:time(10 second) where seller = 'AAA' and amount > 5 having sum(amount) > 10 group by seller 
  private String timeSeries;
  // Ex: timeSeries = "10 second" --> in esper: select sum(amount) from new_order.win:time(10 second)
  private String conditions;
  // Ex: sum(amount) > 100 & count(txId) > 5

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

  public String getConditions() {
    return conditions;
  }

  public void setConditions(String conditions) {
    this.conditions = conditions;
  }

  public String getRuleName() {
    return ruleName;
  }

  public void setRuleName(String ruleName) {
    this.ruleName = ruleName;
  }
}

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

  public static enum EventQueryStatus {
    STOP, RUNNING
  }
  
  public static enum EventQueryType {
    DEFAULT, DAY_OF_WEEK
  }
  
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
  private String smallBucket;// for every timeframe
  // Ex: timeSeries = "10 second" --> in esper: select sum(amount) from new_order.win_batch:time(10 second)
  private String bigBucket;// for last timeframe
  // Ex: timeSeries = "1 minute" --> in esper: select sum(amount) from new_order.win:time(10 minute)
  private String conditions;
  // Ex: sum(amount) > 100 & count(txId) > 5
  private String description;
  private EventQueryStatus status;
  private EventQueryType type;
  private Integer weight;

  public EventQueryType getType() {
    return type;
  }

  public void setType(EventQueryType type) {
    this.type = type;
  }


  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
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

  public String getSmallBucket() {
    return smallBucket;
  }

  public void setSmallBucket(String smallBucket) {
    this.smallBucket = smallBucket;
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

  public String getBigBucket() {
    return bigBucket;
  }

  public void setBigBucket(String bigBucket) {
    this.bigBucket = bigBucket;
  }

  public EventQueryStatus getStatus() {
    return status;
  }

  public void setStatus(EventQueryStatus status) {
    this.status = status;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}

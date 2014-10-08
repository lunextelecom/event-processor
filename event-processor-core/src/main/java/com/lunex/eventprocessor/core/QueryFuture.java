package com.lunex.eventprocessor.core;


/**
 * A result of an event query.
 */
public class QueryFuture {

  private Object[] values;
  private EventQuery eventQuery;

  public QueryFuture(Object[] values, EventQuery eventQuery) {
    this.values = values;
    this.eventQuery = eventQuery;
  }

  /**
   * return results from EventQuery. If getValue() is called while there are no result, it should be
   * null or the previous value. Call this function after ResultListener.onEvent() will guarantee to
   * get the current result.
   * 
   * @return array of object
   */
  public Object[] getValues() {
    return null;
  }

  public EventQuery getEventQuery() {
    return eventQuery;
  }

  public void setEventQuery(EventQuery eventQuery) {
    this.eventQuery = eventQuery;
  }
}

package com.lunex.eventprocessor;

/**
 * A result of an event query.
 */
public class QueryFuture {

  public EventQuery getQuery() {
    return null;
  }
  /**
   * return results from EventQuery.  If getValue() is called while there are no result, it should be null
   * or the previous value.  Call this function after ResultListener.onEvent() will guarantee to get the current result.
   * @return array of object
   */
  public Object[] getValue(){
    return null;
  }

}

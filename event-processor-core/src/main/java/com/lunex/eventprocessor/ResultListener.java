package com.lunex.eventprocessor;

/**
 * Extend this interface and bind to output for EventQuery.
 */
public interface ResultListener {

  public QueryFuture getQueryFuture();
  /**
   * This method is fired when EventQuery that
   * @param result
   */
  public void onEvent(Object[] result);
}

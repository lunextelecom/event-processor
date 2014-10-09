package com.lunex.eventprocessor.core.listener;

import com.lunex.eventprocessor.core.QueryFuture;

/**
 * Extend this interface and bind to output for EventQuery.
 */
public interface ResultListener {

  public void setQueryFuture(QueryFuture queryFuture);
  
  public QueryFuture getQueryFuture();

  /**
   * This method is fired when EventQuery that
   * 
   * @param result
   */
  public void onEvent(Object[] result);
}

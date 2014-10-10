package com.lunex.eventprocessor.handler.listener;

import com.espertech.esper.event.map.MapEventBean;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example console output writer
 */
public class ConsoleOutput implements ResultListener {

  static final Logger logger = LoggerFactory.getLogger(ConsoleOutput.class);

  private QueryFuture queryFuture;

  public void setQueryFuture(QueryFuture queryFuture) {
    this.queryFuture = queryFuture;
  }

  public QueryFuture getQueryFuture() {
    return queryFuture;
  }

  public void onEvent(Object[] result) {
    // **************************************//
    // * Only show result on console screen *//
    // **************************************//
    for (int i = 0; i < result.length; i++) {
      MapEventBean item = (MapEventBean) result[i];
      System.out.println(item.getProperties().toString());
    }
  }
}

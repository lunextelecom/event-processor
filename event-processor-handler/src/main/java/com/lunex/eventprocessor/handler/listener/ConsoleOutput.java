package com.lunex.eventprocessor.handler.listener;

import java.util.Date;
import java.util.Map;

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
      Map<String, Object> item = (Map<String, Object>) result[i];
      System.out.println(item.toString() + " - " + new Date());
    }
  }
}

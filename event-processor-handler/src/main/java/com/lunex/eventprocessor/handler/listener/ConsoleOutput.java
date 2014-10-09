package com.lunex.eventprocessor.handler.listener;

import com.espertech.esper.event.map.MapEventBean;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;


/**
 * Example console output writer
 */
public class ConsoleOutput implements ResultListener {

  private QueryFuture queryFuture;

  public QueryFuture getQueryFuture() {
    return queryFuture;
  }

  public void onEvent(Object[] result) {
    for (int i = 0; i < result.length; i++) {
      MapEventBean item = (MapEventBean) result[i];
      System.out.println(item.getProperties().toString());
    }
  }

}

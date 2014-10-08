package com.lunex.eventprocessor.handler.listener;

import com.espertech.esper.event.map.MapEventBean;
import com.espertech.esper.event.map.MapEventType;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;


/**
 * Example console output writer
 */
public class ConsoleOutput implements ResultListener {

  public void onEvent(QueryFuture result) {
    if (result == null || result.getValues() == null) {
      return;
    }
    Object[] objects = result.getValues();
    for (int i = 0; i < objects.length; i++) {
      MapEventBean item = (MapEventBean) objects[i];
      System.out.println(item.getProperties().toString());
    }
  }

}

package com.lunex.eventprocessor.handler.listener;

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
      System.out.println(objects[0].toString());
    }
  }

}

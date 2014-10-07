package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.IResultListener;

public class ConsoleOutput implements IResultListener {

  public void onEvent(QueryFuture result) {
    Object[] objects = result.getValue();
    for (int i = 0; i < objects.length; i++) {
      System.out.println(objects[0].toString());
    }
  }

}

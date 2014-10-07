package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.Event;

public class EsperProcessor implements Processor {
  
  public void consume(Event event) {
    System.out.println("Start consume event:"  + event.toString());
  }

  public QueryHierarchy getHierarchy() {
    return null;
  }

  public void setHierarchy(QueryHierarchy hierarchy) {}
}

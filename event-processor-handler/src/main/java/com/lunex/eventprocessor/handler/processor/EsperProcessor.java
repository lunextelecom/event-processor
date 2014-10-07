package com.lunex.eventprocessor.handler.processor;

import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.handler.reader.QueryHierarchy;

public class EsperProcessor implements Processor {
  
  public void consume(Event event) {
    System.out.println("Start consume event:"  + event.toString());
  }

  public QueryHierarchy getHierarchy() {
    return null;
  }

  public void setHierarchy(QueryHierarchy hierarchy) {}
}

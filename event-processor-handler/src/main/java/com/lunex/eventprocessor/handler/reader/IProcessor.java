package com.lunex.eventprocessor.handler.reader;

public interface IProcessor extends IEventConsumer {
  
  public QueryHierarchy getHierarchy();

  public void setHierarchy(QueryHierarchy hierarchy);
}

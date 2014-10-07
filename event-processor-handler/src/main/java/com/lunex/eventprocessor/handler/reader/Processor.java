package com.lunex.eventprocessor.handler.reader;

public interface Processor extends EventConsumer {
  
  /**
   * Get Hierarchy 
   * @return
   */
  public QueryHierarchy getHierarchy();

  /**
   * Set Hierarchy
   * @param hierarchy
   */
  public void setHierarchy(QueryHierarchy hierarchy);
}

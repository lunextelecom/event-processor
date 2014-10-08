package com.lunex.eventprocessor.handler.processor;

import com.lunex.eventprocessor.core.QueryHierarchy;

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

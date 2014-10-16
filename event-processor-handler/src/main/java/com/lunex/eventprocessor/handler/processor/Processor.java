package com.lunex.eventprocessor.handler.processor;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryHierarchy;

public interface Processor extends EventConsumer {

  /**
   * Get Hierarchy
   * 
   * @return
   */
  public QueryHierarchy getHierarchy();

  /**
   * Set Hierarchy
   * 
   * @param hierarchy
   */
  public void setHierarchy(QueryHierarchy hierarchy);

  /**
   * Update eventQuery
   * 
   * @param eventQuery
   */
  public boolean updateRule(EventQuery eventQuery, boolean backfill, long backFillTime);

  /**
   * Start EventQuery
   * 
   * @param eventQuery
   * @param backfill
   * @param backFillTime
   * @return
   */
  public boolean startRule(EventQuery eventQuery, boolean backfill, long backFillTime);


  /**
   * Stop EventQuery
   * 
   * @param eventQuery
   * @return
   */
  public boolean stopRule(EventQuery eventQuery);
}

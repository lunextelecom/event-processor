package com.lunex.eventprocessor.handler.processor;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryHierarchy;

// TODO: Auto-generated Javadoc
/**
 * The Interface Processor.
 */
public interface Processor extends EventConsumer {

  /**
   * Get Hierarchy.
   *
   * @return the hierarchy
   */
  public QueryHierarchy getHierarchy();

  /**
   * Set Hierarchy.
   *
   * @param hierarchy the hierarchy
   */
  public void setHierarchy(QueryHierarchy hierarchy);

  /**
   * Update eventQuery.
   *
   * @param eventQuery the event query
   * @param backfill the backfill
   * @param backFillTime the back fill time
   * @return true, if update rule
   */
  public boolean updateRule(EventQuery eventQuery, boolean backfill, long backFillTime);

  /**
   * Start EventQuery.
   *
   * @param eventQuery the event query
   * @param backfill the backfill
   * @param backFillTime the back fill time
   * @return true, if start rule
   */
  public boolean startRule(EventQuery eventQuery, boolean backfill, long backFillTime);


  /**
   * Stop EventQuery.
   *
   * @param eventQuery the event query
   * @return true, if stop rule
   */
  public boolean stopRule(EventQuery eventQuery);
  
  /**
   * Reprocess.
   *
   * @param eventQuery the event query
   * @param backfill the backfill
   * @param backFillTime the back fill time
   * @return true, if reprocess
   */
  public boolean reprocess(EventQuery eventQuery, boolean backfill, long backFillTime);
}

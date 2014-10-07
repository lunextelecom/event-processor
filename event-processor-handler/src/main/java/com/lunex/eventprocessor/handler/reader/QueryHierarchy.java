package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;
import java.util.Map;

public class QueryHierarchy {

  /**
   * add Query
   * 
   * @param evtName
   * @param query
   * @param outputs
   * @return
   */
  public QueryFuture addQuery(String evtName, EventQuery query, ResultListener[] outputs) {
    return null;
  }

  /**
   * Get Hierarchy
   * 
   * @return
   */
  public Map<String, Map<EventQuery, ResultListener[]>> getHierarchy() {
    return null;
  }

  /**
   * Get Future
   * 
   * @param query
   * @return
   */
  public QueryFuture getFuture(EventQuery query) {
    return null;
  }

  /**
   * bindOutput
   * 
   * @param query
   * @param outputs
   */
  public void bindOutput(QueryFuture query, ResultListener[] outputs) {}
}

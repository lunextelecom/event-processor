package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Provide relationship between evtname, EventQuery and ResultListender.
 */
public class QueryHierarchy {

  private Map<String, Map<EventQuery, ResultListener[]>> hierarchy;

  public QueryHierarchy() {
    this.hierarchy = new HashMap<String, Map<EventQuery, ResultListener[]>>();
  }

  /**
   * add Query
   * 
   * @param evtName
   * @param query
   * @param outputs
   * @return
   */
  public QueryFuture addQuery(String evtName, EventQuery query, ResultListener[] outputs) {
    Map<EventQuery, ResultListener[]> temp = hierarchy.get(evtName);
    if (temp == null) {
      temp = new HashMap<EventQuery, ResultListener[]>();
    } else {
      temp.put(query, outputs);
    }
    hierarchy.put(evtName, temp);
    return null;
  }

  /**
   * Get Hierarchy
   * 
   * @return
   */
  public Map<String, Map<EventQuery, ResultListener[]>> getHierarchy() {
    return hierarchy;
  }

  /**
   * Get Future
   * 
   * @param query
   * @return
   */
  public QueryFuture getFuture(EventQuery query) {
    return query.getFuture();
  }

  /**
   * bindOutput: RuleListener process QueryFuture
   * 
   * @param query
   * @param outputs
   */
  public void bindOutput(QueryFuture query, ResultListener[] outputs) {
    for (int i = 0, length = outputs.length; i < length; i++) {
      outputs[i].onEvent(query);
    }
  }
}

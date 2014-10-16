package com.lunex.eventprocessor.core;

import com.lunex.eventprocessor.core.listener.ResultListener;

import java.util.HashMap;
import java.util.Iterator;
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
    }
    temp.put(query, outputs);
    hierarchy.put(evtName, temp);
    return null;
  }

  /**
   * Remove Event query from Hierarchy
   * 
   * @param eventQuery
   */
  public void removeQueryHierarchy(String evtName, EventQuery eventQuery) {
    Map<EventQuery, ResultListener[]> listeners = hierarchy.get(evtName);
    Iterator<EventQuery> it = listeners.keySet().iterator();
    while (it.hasNext()) {
      EventQuery temp = it.next();
      if (temp.getEventName().equals(eventQuery.getEventName())
          && temp.getRuleName().equals(eventQuery.getRuleName())) {
        // if (eventQuery == temp) {
        listeners.remove(temp);
        break;
      }
    }
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
    return null;
  }

  /**
   * bindOutput: RuleListener process QueryFuture
   * 
   * @param query
   * @param outputs
   */
  public void bindOutput(QueryFuture query, ResultListener[] outputs) {
    if (query == null) {
      return;
    }
    if (outputs == null || outputs.length == 0) {
      return;
    }
    for (int i = 0, length = outputs.length; i < length; i++) {
      outputs[i].setQueryFuture(query);
      outputs[i].onEvent(query.getValues());
    }
  }
}

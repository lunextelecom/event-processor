package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.IResultListener;
import java.util.Map;

public class QueryHierarchy {
  public QueryFuture addQuery(String evtName, EventQuery query, IResultListener[] outputs) {
    return null;
  }

  public Map<String, Map<EventQuery, IResultListener[]>> getHierarchy() {
    return null;
  }

  public QueryFuture getFuture(EventQuery query) {
    return null;
  }

  public void bindOutput(QueryFuture query, IResultListener[] outputs) {}
}

package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;
import java.util.Map;

public class QueryHierarchy {
  public QueryFuture addQuery(String evtName, EventQuery query, ResultListener[] outputs) {
    return null;
  }

  public Map<String, Map<EventQuery, ResultListener[]>> getHierarchy() {
    return null;
  }

  public QueryFuture getFuture(EventQuery query) {
    return null;
  }

  public void bindOutput(QueryFuture query, ResultListener[] outputs) {}
}

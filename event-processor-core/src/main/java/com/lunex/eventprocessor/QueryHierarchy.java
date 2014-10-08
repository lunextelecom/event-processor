package com.lunex.eventprocessor;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Map;

/**
 * Provide relationship between evtname, EventQuery and ResultListender.
 */
public class QueryHierarchy {

  public QueryFuture addQuery(String evtName, EventQuery query, ResultListener[] outputs){
    throw new NotImplementedException();
  }

  public Map<String, Map<EventQuery, ResultListener[]>> getHierarchy(){
    throw new NotImplementedException();
  }

  public QueryFuture getFuture(EventQuery query){
    throw new NotImplementedException();
  }

  public void bindOutput(QueryFuture query, ResultListener[] outputs){

  }
}

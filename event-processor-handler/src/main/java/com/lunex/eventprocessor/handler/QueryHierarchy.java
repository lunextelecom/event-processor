package com.lunex.eventprocessor.handler;

import com.lunex.eventprocessor.EventQuery;
import com.lunex.eventprocessor.QueryFuture;
import com.lunex.eventprocessor.ResultListener;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Map;

/**
 * Created by jerryj on 10/6/14.
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

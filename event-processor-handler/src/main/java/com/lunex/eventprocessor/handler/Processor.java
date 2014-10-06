package com.lunex.eventprocessor.handler;

import com.lunex.eventprocessor.EventQuery;
import com.lunex.eventprocessor.QueryFuture;
import com.lunex.eventprocessor.ResultListener;

import java.util.Map;

/**
 * Created by jerryj on 10/1/14.
 */
public interface Processor extends EventConsumer {

  public QueryFuture addQuery(String evtName, EventQuery query);

  public Map<String, Map<EventQuery, ResultListener>> getHierarchy();

  public QueryFuture getFuture(EventQuery query);

  public void bindOutput(QueryFuture future, ResultListener[] output);


}

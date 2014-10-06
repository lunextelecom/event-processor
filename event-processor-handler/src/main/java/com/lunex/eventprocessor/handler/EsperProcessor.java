package com.lunex.eventprocessor.handler;

import com.lunex.eventprocessor.Event;
import com.lunex.eventprocessor.EventQuery;
import com.lunex.eventprocessor.QueryFuture;
import com.lunex.eventprocessor.ResultListener;

import java.util.Map;

/**
 * Created by jerryj on 10/2/14.
 */
public class EsperProcessor implements Processor{


  @Override
  public QueryFuture addQuery(String evtName, EventQuery query) {
    return null;
  }

  @Override
  public Map<String, Map<EventQuery, ResultListener>> getHierarchy() {
    return null;
  }

  @Override
  public QueryFuture getFuture(EventQuery query) {
    return null;
  }

  @Override
  public void bindOutput(QueryFuture future, ResultListener[] output) {

  }


  @Override
  public void consume(Event event) {

  }
}

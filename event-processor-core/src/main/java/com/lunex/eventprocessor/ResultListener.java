package com.lunex.eventprocessor;

/**
 * Created by jerryj on 10/1/14.
 */
public interface ResultListener {

  public void onEvent(QueryFuture result);
}

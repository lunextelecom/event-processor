package com.lunex.eventprocessor.core;

public interface IResultListener {
  public void onEvent(QueryFuture result);
}

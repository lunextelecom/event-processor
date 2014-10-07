package com.lunex.eventprocessor.core.listener;

import com.lunex.eventprocessor.core.QueryFuture;

public interface ResultListener {
  public void onEvent(QueryFuture result);
}

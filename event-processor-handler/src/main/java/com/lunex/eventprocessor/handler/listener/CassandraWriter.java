package com.lunex.eventprocessor.handler.listener;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;

public class CassandraWriter implements ResultListener {

  private QueryFuture queryFuture;

  public void setQueryFuture(QueryFuture queryFuture) {
    this.queryFuture = queryFuture;
  }

  public QueryFuture getQueryFuture() {
    return this.queryFuture;
  }

  public void onEvent(Object[] result) {
    // *******************************//
    // * Write result into cassandra *//
    // *******************************//
    EventQuery eventQuery = null;
    if (queryFuture != null) {
      eventQuery = queryFuture.getEventQuery();
    } else {
      return;
    }
    DataAccessOutputHandler.writeResultToCassandra(result, eventQuery);
  }

}

package com.lunex.eventprocessor.handler.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;

public class KairosDBWriter implements ResultListener {
  static final Logger logger = LoggerFactory.getLogger(KairosDBWriter.class);
  private QueryFuture queryFuture;

  public void setQueryFuture(QueryFuture queryFuture) {
    this.queryFuture = queryFuture;
  }

  public QueryFuture getQueryFuture() {
    return this.queryFuture;
  }

  public void onEvent(Object[] result) {
    // ******************************//
    // * Write result into KairosDB *//
    // ******************************//

    if (queryFuture != null) {
      final EventQuery eventQuery = queryFuture.getEventQuery();
      final Object[] data = result;
      Thread kairosDBWriter = new Thread(new Runnable() {
        public void run() {
          DataAccessOutputHandler.writeResultToKairosDB(data, eventQuery);
        }
      });
      kairosDBWriter.start();
    }
  }
}

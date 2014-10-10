package com.lunex.eventprocessor.handler.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.event.map.MapEventBean;
import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.dataaccess.KairosDBClient;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;
import com.lunex.eventprocessor.handler.utils.Configurations;

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

    EventQuery eventQuery = null;
    if (queryFuture != null) {
      eventQuery = queryFuture.getEventQuery();
    } else {
      return;
    }
    DataAccessOutputHandler.writeResultToKairosDB(result, eventQuery);
    
  }
}

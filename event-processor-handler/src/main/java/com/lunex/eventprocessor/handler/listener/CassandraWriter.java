package com.lunex.eventprocessor.handler.listener;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;
import com.lunex.eventprocessor.handler.utils.Configurations;

public class CassandraWriter implements ResultListener {

  static final Logger logger = LoggerFactory.getLogger(CassandraWriter.class);

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
    try {
      // Write result computation
      Map<String, Object> properties = null;
      String hashKey = null;
      for (int i = 0; i < result.length; i++) {
        properties = (Map<String, Object>) result[i];
        hashKey =
            String.valueOf(properties.get("hashKey") == null ? Constants.EMPTY_STRING : properties
                .get("hashKey"));
        if (properties == null || properties.isEmpty() || Constants.EMPTY_STRING.equals(hashKey)) {
          continue;
        }
        // Write result of computation
        DataAccessOutputHandler.writeResultComputation(properties, eventQuery);
      }
      // Write checked condition
      EventResult eventResult = DataAccessOutputHandler.checkCondition(result, eventQuery);
      CassandraRepository.getInstance(Configurations.cassandraHost, Configurations.cassandraKeyspace).updateResults(eventResult);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

}

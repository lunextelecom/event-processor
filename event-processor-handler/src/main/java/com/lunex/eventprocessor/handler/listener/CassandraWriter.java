package com.lunex.eventprocessor.handler.listener;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.EventQuery.EventQueryType;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.handler.output.CheckConditionDayOfWeek;
import com.lunex.eventprocessor.handler.output.CheckConditionDefault;
import com.lunex.eventprocessor.handler.output.CheckConditionHandler;
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
    if (queryFuture != null) {
      final EventQuery eventQuery = queryFuture.getEventQuery();
      final Object[] data = result;
      Thread cassandraWriter = new Thread(new Runnable() {
        public void run() {
          try {
            // Write result of computation
            DataAccessOutputHandler.writeResultComputation(data, eventQuery);

            CheckConditionHandler checkConditionHandler = new CheckConditionDefault();
            if (eventQuery != null && eventQuery.getType() == EventQueryType.DAY_OF_WEEK) {
              checkConditionHandler = new CheckConditionDayOfWeek();
            }
            // Write checked condition
            EventResult eventResult =
                DataAccessOutputHandler.checkCondition(data, eventQuery, checkConditionHandler);
            CassandraRepository.getInstance(Configurations.cassandraHost,
                Configurations.cassandraKeyspace).updateResults(eventResult);
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        }
      });
      cassandraWriter.start();
    }
  }

}

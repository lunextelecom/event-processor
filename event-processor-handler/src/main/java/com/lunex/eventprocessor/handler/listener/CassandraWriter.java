package com.lunex.eventprocessor.handler.listener;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.QueryFuture;
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
      if (eventQuery == null) {
        return;
      }
      final Object[] data = result;
      Thread cassandraWriter = new Thread(new Runnable() {
        public void run() {
          try {
            // Write result of computation
            //DataAccessOutputHandler.writeResultComputation(data, eventQuery);

            // Write checked condition
            CheckConditionHandler checkConditionHandler = null;
            switch (eventQuery.getType()) {
              case DEFAULT:
                checkConditionHandler = new CheckConditionDefault();
                break;
              case DAY_OF_WEEK:
                checkConditionHandler = new CheckConditionDayOfWeek();
                break;
              default:
                checkConditionHandler = new CheckConditionDefault();
                break;
            }
            List<EventResult> eventResults =
                DataAccessOutputHandler.checkCondition(data, eventQuery, checkConditionHandler);
            EventResult eventResult = null;
            for (int i = 0; i < eventResults.size(); i++) {
              eventResult = eventResults.get(i);
              CassandraRepository.getInstance(Configurations.cassandraHost,
                  Configurations.cassandraKeyspace).updateResults(eventResult);
            }
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        }
      });
      cassandraWriter.start();
    }
  }

}

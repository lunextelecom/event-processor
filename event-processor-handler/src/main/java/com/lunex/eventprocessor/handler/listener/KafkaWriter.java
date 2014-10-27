package com.lunex.eventprocessor.handler.listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.handler.EventHandlerLaunch;
import com.lunex.eventprocessor.handler.output.CheckConditionDayOfWeek;
import com.lunex.eventprocessor.handler.output.CheckConditionDefault;
import com.lunex.eventprocessor.handler.output.CheckConditionHandler;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;
import com.lunex.eventprocessor.handler.utils.Configurations;

public class KafkaWriter implements ResultListener {

  static final Logger logger = LoggerFactory.getLogger(KafkaWriter.class);

  private QueryFuture queryFuture;

  public void setQueryFuture(QueryFuture queryFuture) {
    this.queryFuture = queryFuture;
  }

  public QueryFuture getQueryFuture() {
    return this.queryFuture;
  }

  public void onEvent(Object[] result) {
    // *******************************//
    // * Write result into kafka *//
    // *******************************//
    if (queryFuture != null) {
      final EventQuery eventQuery = queryFuture.getEventQuery();
      if (eventQuery == null) {
        return;
      }
      final Object[] data = result;
      Thread threadKafkaWriter = new Thread(new Runnable() {
        public void run() {
          try {
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
            Gson gson = new Gson();
            for (int i = 0; i < eventResults.size(); i++) {
              eventResult = eventResults.get(i);
              String json = gson.toJson(eventResult);
              EventHandlerLaunch.kafkaProducer.sendData(Configurations.kafkaTopicOutput,
                  eventQuery.getEventName(), json);
            }
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        }
      });
      threadKafkaWriter.start();
    }
  }
}

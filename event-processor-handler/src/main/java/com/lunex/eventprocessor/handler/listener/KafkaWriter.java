package com.lunex.eventprocessor.handler.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventQuery.EventQueryType;
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
    EventQuery eventQuery = null;
    if (queryFuture != null) {
      eventQuery = queryFuture.getEventQuery();
    } else {
      return;
    }

    try {
      CheckConditionHandler checkConditionHandler = new CheckConditionDefault();
      if(eventQuery != null && eventQuery.getType()==EventQueryType.DAY_OF_WEEK){
        checkConditionHandler = new CheckConditionDayOfWeek();
      }
      EventResult eventResult = DataAccessOutputHandler.checkCondition(result, eventQuery, checkConditionHandler);
      Gson gson = new Gson();
      String json = gson.toJson(eventResult);
      EventHandlerLaunch.kafkaProducer.sendData(Configurations.kafkaTopicOutput, eventQuery.getEventName(), json);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }
}

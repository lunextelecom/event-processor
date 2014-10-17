package com.lunex.eventprocessor.handler.listener;

import kafka.serializer.DefaultEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.handler.kafka.ASCIIPartitioner;
import com.lunex.eventprocessor.handler.kafka.KafkaProducer;
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
      EventResult eventResult = DataAccessOutputHandler.checkCondition(result, eventQuery);
      Gson gson = new Gson();
      String json = gson.toJson(eventResult);
      KafkaProducer kafkaProducer =
          new KafkaProducer(Configurations.kafkaCluster, DefaultEncoder.class.getName(),
              ASCIIPartitioner.class.getName(), true);
      kafkaProducer.sendData(Configurations.kafkaTopicOutput, eventQuery.getEventName(), json);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }
}

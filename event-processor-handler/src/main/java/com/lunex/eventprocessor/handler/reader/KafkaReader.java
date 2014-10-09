package com.lunex.eventprocessor.handler.reader;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.handler.kafka.KafkaMessageProcessor;
import com.lunex.eventprocessor.handler.kafka.KafkaSimpleConsumer;
import com.lunex.eventprocessor.handler.processor.EventConsumer;
import com.lunex.eventprocessor.handler.utils.Configurations;

public class KafkaReader implements EventReader {

  static final Logger logger = LoggerFactory.getLogger(KafkaReader.class);

  private List<KafkaSimpleConsumer> listConsumers;
  private int partitionIndex = -1;

  /**
   * Contructor
   * 
   * @param partitionIndex : if = -1 --> read message from all partion of topic
   */
  public KafkaReader(int partitionIndex) {
    this.listConsumers = new ArrayList<KafkaSimpleConsumer>();
    this.partitionIndex = partitionIndex;
  }

  public Event readNext() {
    // no implement this function for KafkaReader
    return null;
  }

  public void read(final EventConsumer consumer) {
    if (partitionIndex == -1) {
      for (int i = 0; i < Configurations.kafkaTopicNumPartition; i++) {
        final KafkaSimpleConsumer kafkaConsumer =
            new KafkaSimpleConsumer(Configurations.kafkaCluster, Configurations.kafkaTopic, i, -1,
                new KafkaMessageProcessor() {
                  public Object processMessage(byte[] message) {
                    sendEventToConsumer(message, consumer);
                    return null;
                  }
                });
        listConsumers.add(kafkaConsumer);
        try {
          Thread thread = new Thread(new Runnable() {
            public void run() {
              try {
                kafkaConsumer.readKafka(kafka.api.OffsetRequest.LatestTime());
              } catch (Exception e) {
                logger.error("Function read: " + e.getMessage());
              }
            }
          });
          thread.start();
        } catch (Exception e) {
          logger.error("Function read: " + e.getMessage());
        }
      }
    } else {
      KafkaSimpleConsumer kafkaConsumer =
          new KafkaSimpleConsumer(Configurations.kafkaCluster, Configurations.kafkaTopic,
              partitionIndex, -1, new KafkaMessageProcessor() {
                public Object processMessage(byte[] message) {
                  sendEventToConsumer(message, consumer);
                  return null;
                }
              });
      listConsumers.add(kafkaConsumer);
      try {
        kafkaConsumer.readKafka(kafka.api.OffsetRequest.LatestTime());
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    }
  }

  public void stop() {
    for (int i = 0; i < listConsumers.size(); i++) {
      listConsumers.get(i).stoped = true;
    }
  }

  /**
   * Send message event to Consumer to consume event
   * 
   * @param message
   * @param consumer
   */
  public void sendEventToConsumer(byte[] message, final EventConsumer consumer) {
    Event event = null;
    // read 1st byte to get content-type of payload
    byte contentype = message[0];
    switch (contentype) {
      case 1: // JSON
        message = Arrays.copyOfRange(message, 1, message.length);
        try {
          String payload = new String(message, "UTF-8");
          if (!StringUtils.isJSONValid(payload)) {
            logger.error("Invalid Json");
            return;
          }
          event = new Event(System.currentTimeMillis(), payload);
        } catch (UnsupportedEncodingException e) {
          event = null;
          logger.error(e.getMessage());
        }
        break;
      default:
        logger.error("Content-type is invalid");
        break;
    }
    if (event != null) {
      consumer.consume(event);
    }
  }

}

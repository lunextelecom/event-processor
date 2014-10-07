package com.lunex.eventprocessor.handler.reader;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.handler.kafka.KafkaMessageProcessor;
import com.lunex.eventprocessor.handler.kafka.KafkaSimpleConsumer;
import com.lunex.eventprocessor.handler.utils.Configuration;

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
      for (int i = 0; i < Configuration.kafkaTopicNumPartition; i++) {
        final KafkaSimpleConsumer kafkaConsumer =
            new KafkaSimpleConsumer(Configuration.kafkaCluster, Configuration.kafkaTopic, i, -1,
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
              }
            }
          });
          thread.start();
        } catch (Exception e) {
          logger.error(e.getMessage());
        }
      }
    } else {
      KafkaSimpleConsumer kafkaConsumer =
          new KafkaSimpleConsumer(Configuration.kafkaCluster, Configuration.kafkaTopic,
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

  public void sendEventToConsumer(byte[] message, final EventConsumer consumer) {
    Event event = null;
    byte contentype = message[0];
    switch (contentype) {
      case 1:
        message = Arrays.copyOfRange(message, 1, message.length);
        try {
          event = new Event(new String(message, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
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

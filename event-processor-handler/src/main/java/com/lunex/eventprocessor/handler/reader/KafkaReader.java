package com.lunex.eventprocessor.handler.reader;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.handler.kafka.KafkaMessageProcessor;
import com.lunex.eventprocessor.handler.kafka.KafkaSimpleConsumer;
import com.lunex.eventprocessor.handler.processor.EventConsumer;
import com.lunex.eventprocessor.handler.utils.Configurations;

public class KafkaReader implements EventReader {

  static final Logger logger = LoggerFactory.getLogger(KafkaReader.class);

  private List<KafkaSimpleConsumer> listKafkaConsumers;
  private EventConsumer consumer;
  private long whichTime = kafka.api.OffsetRequest.LatestTime();

  /**
   * Contructor
   * 
   * @param partitionIndex : if = -1 --> read message from all partion of topic
   */
  public KafkaReader() {
    this.listKafkaConsumers = new ArrayList<KafkaSimpleConsumer>();
  }
  
  public KafkaReader(long whichTime) {
    this.whichTime = whichTime;
    this.listKafkaConsumers = new ArrayList<KafkaSimpleConsumer>();
  }

  public Event readNext() {
    // no implement this function for KafkaReader
    return null;
  }

  public void read(EventConsumer consumer) {
    this.consumer = consumer;
    this.readKafkaMessage(consumer);
  }

  public void stop() {
    for (int i = 0; i < listKafkaConsumers.size(); i++) {
      listKafkaConsumers.get(i).stoped = true;
    }
  }

  public void start() {
    //listKafkaConsumers.clear();
    //readKafkaMessage(consumer);
    for (int i = 0; i < listKafkaConsumers.size(); i++) {
      listKafkaConsumers.get(i).stoped = false;
      final KafkaSimpleConsumer kafkaConsumer = listKafkaConsumers.get(i);
      try {
        Thread thread = new Thread(new Runnable() {
          public void run() {
            try {
              String target = "19-10-2014 20:29:30";
              DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
              Date result =  df.parse(target);
              whichTime = result.getTime();
              kafkaConsumer.readKafka(whichTime);
            } catch (Exception e) {
              logger.error("Function read: " + e.getMessage(), e);
            }
          }
        });
        thread.start();
      } catch (Exception e) {
        logger.error("Function read: " + e.getMessage(), e);
      }
    }
  }

  private void readKafkaMessage(final EventConsumer consumer) {
    for (int i = 0; i < Configurations.kafkaTopicPartitionList.size(); i++) {
      final KafkaSimpleConsumer kafkaConsumer =
          new KafkaSimpleConsumer(Configurations.kafkaCluster, Configurations.kafkaTopic,
              Configurations.kafkaTopicPartitionList.get(i), -1, new KafkaMessageProcessor() {
                public Object processMessage(byte[] message) {
                  sendEventToConsumer(message, consumer);
                  return null;
                }
              });
      listKafkaConsumers.add(kafkaConsumer);
      try {
        Thread thread = new Thread(new Runnable() {
          public void run() {
            try {
              String target = "19-10-2014 20:29:30";
              DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
              Date result =  df.parse(target);
              whichTime = result.getTime();
              kafkaConsumer.readKafka(whichTime);
            } catch (Exception e) {
              logger.error("Function read: " + e.getMessage(), e);
            }
          }
        });
        thread.start();
      } catch (Exception e) {
        logger.error("Function read: " + e.getMessage(), e);
      }
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
          logger.error(e.getMessage(), e);
        }
        break;
      default:
        logger.error("Content-type is invalid");
        break;
    }
    if (Configurations.kafkaEventReaderList != null
        && !Configurations.kafkaEventReaderList.isEmpty()
        && !Configurations.kafkaEventReaderList.contains(event.getEvtName())) {
      logger.error("Invalid event name");
      return;
    }
    if (event != null && event.getTime() != -1
        && !Constants.EMPTY_STRING.equals(event.getEvtName())
        && !Constants.EMPTY_STRING.equals(event.getEvent())) {
      consumer.consume(event);
    }
  }
}

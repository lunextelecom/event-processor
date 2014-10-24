package com.lunex.eventprocessor.handler.reader;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import joptsimple.internal.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.core.utils.TimeUtil;
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
   */
  public KafkaReader() {
    this.listKafkaConsumers = new ArrayList<KafkaSimpleConsumer>();
  }

  /**
   * Create instance to read kafka messge from time
   * 
   * @param whichTime 1002345043064
   */
  public KafkaReader(long whichTime) {
    this.whichTime = whichTime;
    this.listKafkaConsumers = new ArrayList<KafkaSimpleConsumer>();
  }

  /**
   * Constructor Create instance to read kafka messge from time
   * 
   * @param time: 1 day, 2 day, 4 hour, 1 month
   */
  public KafkaReader(String backTime) {
    this.whichTime = StringUtils.getBackFillTime(backTime);
    this.listKafkaConsumers = new ArrayList<KafkaSimpleConsumer>();
  }

  public KafkaReader(String dateTime, String formatTime) {
    if (Strings.isNullOrEmpty(formatTime.trim())) {
      formatTime = "dd-MM-yyyy HH:mm:ss z";
    }
    Date date = TimeUtil.convertStringToDate(dateTime, formatTime);
    // System.out.println("Kafka reader start: _____________________" + date
    // + "________________________");
    this.whichTime = TimeUtil.convertDateToUnixTime(date);
    // System.out.println("Kafka reader start whichTime: _____________________" + whichTime
    // + "________________________");
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
    // listKafkaConsumers.clear();
    // readKafkaMessage(consumer);
    for (int i = 0; i < listKafkaConsumers.size(); i++) {
      listKafkaConsumers.get(i).stoped = false;
      final KafkaSimpleConsumer kafkaConsumer = listKafkaConsumers.get(i);
      try {
        Thread thread = new Thread(new Runnable() {
          public void run() {
            try {
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
          // Event payload must contain evtName, and time
          event = new Event(payload);
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
      logger.error("Invalid event name: " + event.getEvtName());
      return;
    }
    if (event != null && event.getTime() != -1
        && !Constants.EMPTY_STRING.equals(event.getEvtName())
        && !Constants.EMPTY_STRING.equals(event.getEvent())) {
      consumer.consume(event);
    }
  }

  public EventConsumer getConsumer() {
    return consumer;
  }

  public void setConsumer(EventConsumer consumer) {
    this.consumer = consumer;
  }
}

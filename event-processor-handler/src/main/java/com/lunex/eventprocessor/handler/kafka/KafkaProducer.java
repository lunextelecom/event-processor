package com.lunex.eventprocessor.handler.kafka;

import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

/**
 * 
 * Class Kafka producer to send message to kafka
 *
 */
public class KafkaProducer {

  static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

  private Producer<String, byte[]> producer;

  /**
   * Constructor
   * 
   * @param listBroker : list node of kafka
   * @param serializerForKey: serializer for Key
   * @param partitionerClass: partition Class
   * @param async : default true
   */
  public KafkaProducer(List<String> listBroker, String serializerForKey, String partitionerClass,
      boolean async) {

    Properties props = new Properties();
    props.put("metadata.broker.list", Joiner.on(",").join(listBroker));
    props.put("serializer.class", "kafka.serializer.DefaultEncoder");
    props.put("key.serializer.class", serializerForKey);
    props.put("partitioner.class", partitionerClass);
    props.put("request.required.acks", "1");
    props.put("compression.codec", "gzip");
    if (async) {
      props.put("producer.type", "async");
    }
    ProducerConfig config = new ProducerConfig(props);

    producer = new Producer<String, byte[]>(config);
  }

  /**
   * Send Data as a String message
   * by default, if topic is not existed, kafka create new topic automatically with 2 partition
   * 
   * @param topicName
   * @param key
   * @param message
   * @throws Exception
   */
  public void sendData(String topicName, String key, String message) throws Exception {
    if ("".equals(topicName)) {
      throw new Exception(new Throwable("Topic name is empty"));
    }
    try {
      byte[] byteArray = message.getBytes("UTF-8");
      KeyedMessage<String, byte[]> data =
          new KeyedMessage<String, byte[]>(topicName, key, byteArray);
      producer.send(data);
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw ex;
    }
  }

  public void close() {
    this.producer.close();
  }
}

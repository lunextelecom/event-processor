package com.lunex.eventprocessor.input.kafka;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.lunex.eventprocessor.input.exception.InternalServerErrorException;
import com.lunex.eventprocessor.input.netty.ContentTypeEnum;
import com.lunex.eventprocessor.input.utils.Constant;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.serializer.StringEncoder;

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
   * 
   * @param topicName
   * @param key
   * @param message
   * @throws Exception
   */
  public void sendData(String topicName, String key, String message, ContentTypeEnum contentType)
      throws Exception {
    if (Constant.EMPTY_STRING.equals(topicName)) {
      throw new InternalServerErrorException(new Throwable("Topic name is empty"));
    }
    try {
      byte[] byteArray = message.getBytes(CharsetUtil.UTF_8);
      byteArray = this.addByteContentType(byteArray, contentType);
      KeyedMessage<String, byte[]> data =
          new KeyedMessage<String, byte[]>(topicName, key, byteArray);
      producer.send(data);
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw ex;
    }
  }

  /**
   * Send data as a netty ByteBuf
   * 
   * @param topicName
   * @param key
   * @param byteBuf
   * @throws Exception
   */
  public void sendData(String topicName, String key, ByteBuf byteBuf, ContentTypeEnum contentType)
      throws Exception {
    if (Constant.EMPTY_STRING.equals(topicName)) {
      throw new InternalServerErrorException(new Throwable("Topic name is empty"));
    }
    try {
      byte[] byteArray = byteBuf.array();
      byteArray = this.addByteContentType(byteArray, contentType);
      KeyedMessage<String, byte[]> data =
          new KeyedMessage<String, byte[]>(topicName, key, byteArray);
      producer.send(data);
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw ex;
    }
  }

  /**
   * Send message as a byte array
   * 
   * @param topicName
   * @param key
   * @param byteBuf
   * @throws Exception
   */
  public void sendData(String topicName, String key, byte[] byteBuf, ContentTypeEnum contentType)
      throws Exception {
    if (Constant.EMPTY_STRING.equals(topicName)) {
      throw new InternalServerErrorException(new Throwable("Topic name is empty"));
    }
    try {
      byte[] byteArray = byteBuf;
      byteArray = this.addByteContentType(byteArray, contentType);
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

  /**
   * Add 1 byte for payload to send content-type to consumer
   * 
   * @param payload
   * @param contentType: JSON = 1
   * @return
   */
  public byte[] addByteContentType(byte[] payload, ContentTypeEnum contentType) {
    byte[] newPayload = new byte[payload.length + 1];
    switch (contentType) {
      case JSONType:
        newPayload[0] = (byte) 1;
        break;
      default:
        break;
    }
    for (int i = 0, length = payload.length; i < length; i++) {
      newPayload[i + 1] = payload[i];
    }
    return newPayload;
  }

  public static void main(String[] agrs) {
    List<String> a = new ArrayList<String>();
    a.add("192.168.93.38:9092");
    a.add("192.168.93.39:9092");
    KafkaProducer producer =
        new KafkaProducer(a, StringEncoder.class.getName(), HashCodePartitioner.class.getName(),
            true);
    try {
      producer.sendData("testKafka", "new-order", "kkk", ContentTypeEnum.JSONType);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

package com.lunex.eventprocessor.input.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Joiner;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.serializer.StringEncoder;

public class KafkaProducer {

  Producer<String, String> producer;

  public KafkaProducer(List<String> listBroker, String serializer, String partitionerClass) {

    Properties props = new Properties();
    props.put("metadata.broker.list", Joiner.on(",").join(listBroker));
    props.put("serializer.class", serializer);
    props.put("partitioner.class", partitionerClass);
    props.put("request.required.acks", "1");
    ProducerConfig config = new ProducerConfig(props);

    producer = new Producer<String, String>(config);
  }

  public void sendData(String topicName, String key, String msg) throws Exception {
    try {
      KeyedMessage<String, String> data = new KeyedMessage<String, String>(topicName, key, msg);
      producer.send(data);
    } catch (Exception ex) {
      throw ex;
    }
  }

  public void close() {
    this.producer.close();
  }

  public static void main(String[] agrs) {
    List<String> a = new ArrayList<String>();
    a.add("192.168.93.38:9092");
    a.add("192.168.93.39:9092");
    KafkaProducer producer =
        new KafkaProducer(a, StringEncoder.class.getName(), HashCodePartitioner.class.getName());
    try {
      producer.sendData("testKafka", "new-order", "abc");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

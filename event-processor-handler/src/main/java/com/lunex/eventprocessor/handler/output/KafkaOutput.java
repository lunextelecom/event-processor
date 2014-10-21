package com.lunex.eventprocessor.handler.output;

import com.lunex.eventprocessor.handler.kafka.KafkaProducer;

public class KafkaOutput {

  private KafkaProducer kafkaProducer;

  public KafkaOutput() {

  }

  public KafkaOutput(KafkaProducer kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
  }

  public void writeKafkaOutput(String topic, String key, String content) throws Exception {
    kafkaProducer.sendData(topic, key, content);
  }

}

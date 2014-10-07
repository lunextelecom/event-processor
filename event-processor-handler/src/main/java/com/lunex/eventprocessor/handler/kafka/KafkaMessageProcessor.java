package com.lunex.eventprocessor.handler.kafka;

public interface KafkaMessageProcessor {

  public Object processMessage(byte[] message);
}

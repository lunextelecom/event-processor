package com.lunex.eventprocessor.handler.kafka;

public interface IKafkaMessageProcessor {

  public Object processMessage(byte[] message);
}

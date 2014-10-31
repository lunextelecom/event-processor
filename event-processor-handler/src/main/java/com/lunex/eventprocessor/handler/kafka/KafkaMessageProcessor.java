package com.lunex.eventprocessor.handler.kafka;

/**
 * Interface process read message from kafka
 *
 */
public interface KafkaMessageProcessor {

  public Object processMessage(byte[] message);
}

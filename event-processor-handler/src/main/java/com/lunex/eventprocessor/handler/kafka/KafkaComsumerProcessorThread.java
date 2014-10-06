package com.lunex.eventprocessor.handler.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.message.Message;
import kafka.message.MessageAndMetadata;

public class KafkaComsumerProcessorThread implements Runnable {
  
  static final Logger logger = LoggerFactory.getLogger(KafkaComsumerProcessorThread.class);
  
  private KafkaStream<byte[], byte[]> kafkaStream;
  private int threadNumber;
  private IKafkaMessageProcessor processorMessage;

  public KafkaComsumerProcessorThread(KafkaStream<byte[], byte[]> kafkaStream, int threadNumber, IKafkaMessageProcessor processorMessage) {
    this.threadNumber = threadNumber;
    this.kafkaStream = kafkaStream;
    this.processorMessage = processorMessage;
  }

  public void run() {
    ConsumerIterator<byte[], byte[]> it = kafkaStream.iterator();
    while (it.hasNext()) {
      MessageAndMetadata<byte[], byte[]> message = it.next();
      // TODO somthing
      if (this.processorMessage != null) {
        this.processorMessage.processMessage(message.message());
      }
    }
    System.out.println("Shutting down Thread: " + threadNumber);
  }
}

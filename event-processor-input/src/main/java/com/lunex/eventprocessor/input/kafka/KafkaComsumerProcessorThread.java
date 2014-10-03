package com.lunex.eventprocessor.input.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.message.Message;
import kafka.message.MessageAndMetadata;

public class KafkaComsumerProcessorThread implements Runnable {
  
  static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
  
  private KafkaStream<byte[], byte[]> kafkaStream;
  private int threadNumber;

  public KafkaComsumerProcessorThread(KafkaStream<byte[], byte[]> kafkaStream, int threadNumber) {
    this.threadNumber = threadNumber;
    this.kafkaStream = kafkaStream;
  }

  public void run() {
    ConsumerIterator<byte[], byte[]> it = kafkaStream.iterator();
    while (it.hasNext()) {
      MessageAndMetadata<byte[], byte[]> a = it.next();
      // TODO somthing
    }
    System.out.println("Shutting down Thread: " + threadNumber);
  }
}

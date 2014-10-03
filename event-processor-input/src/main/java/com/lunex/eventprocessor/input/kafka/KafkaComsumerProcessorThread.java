package com.lunex.eventprocessor.input.kafka;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.message.Message;
import kafka.message.MessageAndMetadata;

public class KafkaComsumerProcessorThread implements Runnable {
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
      Object k = a.productElement(0);
      System.out.println("Thread " + threadNumber + ": key - " + new String(a.key()));
      System.out.println("Thread " + threadNumber + ": content" + new String(a.message()));
    }
    System.out.println("Shutting down Thread: " + threadNumber);
  }
}

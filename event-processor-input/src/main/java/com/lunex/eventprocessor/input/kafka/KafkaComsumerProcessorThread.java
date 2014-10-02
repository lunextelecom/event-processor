package com.lunex.eventprocessor.input.kafka;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

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
      System.out.println("Thread " + threadNumber + ": " + new String(it.next().message()));
    }
    System.out.println("Shutting down Thread: " + threadNumber);
  }
}

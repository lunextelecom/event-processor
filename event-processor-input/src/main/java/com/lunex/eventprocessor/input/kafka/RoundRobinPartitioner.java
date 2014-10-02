package com.lunex.eventprocessor.input.kafka;

import java.util.concurrent.atomic.AtomicInteger;

import kafka.producer.Partitioner;
import kafka.utils.VerifiableProperties;

public class RoundRobinPartitioner implements Partitioner<Object> {

  private AtomicInteger currentPartition = new AtomicInteger(0);

  public RoundRobinPartitioner(VerifiableProperties props) {
    this.currentPartition = new AtomicInteger(0);
  }

  public int partition(Object key, int countPartitions) {
    int partition = (this.currentPartition.getAndAdd(1) + countPartitions) % countPartitions;
    return partition;
  }
}

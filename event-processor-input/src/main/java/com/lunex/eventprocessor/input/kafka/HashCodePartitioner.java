package com.lunex.eventprocessor.input.kafka;

import kafka.producer.Partitioner;
import kafka.utils.VerifiableProperties;

public class HashCodePartitioner implements Partitioner {

  public HashCodePartitioner(VerifiableProperties props) {}

  public int partition(Object key, int countPartitions) {
    return (Math.abs(key.hashCode()) % countPartitions);
  }
}

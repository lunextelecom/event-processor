package com.lunex.eventprocessor.input.kafka;

import kafka.producer.Partitioner;
import kafka.utils.VerifiableProperties;

public class ASCIIPartitioner implements Partitioner<Object> {
  public ASCIIPartitioner(VerifiableProperties props) {}

  public int partition(Object key, int countPartitions) {
    int total = 0;
    for (char c : ((String) key).toCharArray())
      total = (int) c;
    return total % countPartitions;
  }
}

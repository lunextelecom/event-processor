package com.lunex.eventprocessor.handler.kafka;

import kafka.producer.Partitioner;
import kafka.utils.VerifiableProperties;

/**
 * Partitioner by ASCII
 *
 */
public class ASCIIPartitioner implements Partitioner {
  public ASCIIPartitioner(VerifiableProperties props) {}

  public int partition(Object key, int countPartitions) {
    int total = 0;
    for (char c : ((String) key).toCharArray()) {
      total += (int) c;
    }
    int partitionIndex = total % countPartitions;
    return partitionIndex;
  }
}
package com.lunex.eventprocessor.input.kafka;

import static org.junit.Assert.*;
import kafka.utils.VerifiableProperties;

import org.junit.Test;

public class RoundRobinPartitionerTest {

  @Test
  public void testPartition() {
    RoundRobinPartitioner partition = new RoundRobinPartitioner(new VerifiableProperties());
    int partitionIndex = partition.partition("", 5);
    assertEquals(0, partitionIndex);
    partitionIndex = partition.partition("", 5);
    assertEquals(1, partitionIndex);
    partitionIndex = partition.partition("", 5);
    assertEquals(2, partitionIndex);
    partitionIndex = partition.partition("", 5);
    assertEquals(3, partitionIndex);
    partitionIndex = partition.partition("", 5);
    assertEquals(4, partitionIndex);
    partitionIndex = partition.partition("", 5);
    assertEquals(0, partitionIndex);
  }

}

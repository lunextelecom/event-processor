package com.lunex.eventprocessor.input.kafka;

import static org.junit.Assert.*;
import kafka.utils.VerifiableProperties;

import org.junit.Test;

public class ASCIIPartitionerTest {

  @Test
  public void testPartition() {
    ASCIIPartitioner aSCIIPartitioner = new ASCIIPartitioner(new VerifiableProperties());
    int partitionIndex = aSCIIPartitioner.partition("ac", 5);
    assertEquals(1, partitionIndex);
  }

}

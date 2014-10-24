package com.lunex.eventprocessor.input.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConfigurationTest {

  @Test
  public void testGetPropertiesValues() {

    try {
      Configuration.getPropertiesValues("conf/app.properties");
    } catch (Exception e) {
      assertEquals(true, false);
    }

    assertEquals(true, Configuration.kafkaCluster.size() > 0);
    assertEquals(true, Configuration.listZookeeper.size() > 0);
    assertEquals(true, !Constant.EMPTY_STRING.equals(Configuration.kafkaTopic));
    assertEquals(true, !Constant.EMPTY_STRING.equals(Configuration.kafkaTopicNumPartition));
    assertEquals(true, !Constant.EMPTY_STRING.equals(Configuration.kafkaProducerAsync));
  }

}

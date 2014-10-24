package com.lunex.eventprocessor.input.kafka;

import static org.junit.Assert.*;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.List;

import kafka.serializer.StringEncoder;

import org.junit.BeforeClass;
import org.junit.Test;

import com.lunex.eventprocessor.input.netty.ContentTypeEnum;

public class KafkaProducerTest {

  static List<String> listNodes;

  @BeforeClass
  public static void beforeClass() {
    listNodes = new ArrayList<String>();
    listNodes.add("192.168.93.38:9092");
    listNodes.add("192.168.93.39:9092");
  }

  @Test
  public void testSendDataStringStringStringContentTypeEnum() {
    KafkaProducer producer =
        new KafkaProducer(listNodes, StringEncoder.class.getName(),
            ASCIIPartitioner.class.getName(), false);
    try {
      producer.sendData("unitTestKafka", "unit_test_key", "kkk", ContentTypeEnum.JSONType);
      assertEquals(true, true);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void testSendDataStringStringByteBufContentTypeEnum() {
    KafkaProducer producer =
        new KafkaProducer(listNodes, StringEncoder.class.getName(),
            ASCIIPartitioner.class.getName(), false);
    try {
      producer.sendData("unitTestKafka", "unit_test_key",
          Unpooled.copiedBuffer("kkk", CharsetUtil.UTF_8), ContentTypeEnum.JSONType);
      assertEquals(true, true);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void testSendDataStringStringByteArrayContentTypeEnum() {
    KafkaProducer producer =
        new KafkaProducer(listNodes, StringEncoder.class.getName(),
            ASCIIPartitioner.class.getName(), false);
    try {
      producer.sendData("unitTestKafka", "unit_test_key", Unpooled.copiedBuffer("kkk".getBytes()),
          ContentTypeEnum.JSONType);
      assertEquals(true, true);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void testAddByteContentType() {
    KafkaProducer producer =
        new KafkaProducer(listNodes, StringEncoder.class.getName(),
            ASCIIPartitioner.class.getName(), false);
    byte[] test = producer.addByteContentType("kkk".getBytes(), ContentTypeEnum.JSONType);
    assertEquals(test[0], 1);
  }
}

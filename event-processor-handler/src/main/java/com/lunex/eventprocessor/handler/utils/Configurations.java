package com.lunex.eventprocessor.handler.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Configurations {

  public static List<String> listZookeeper = new ArrayList<String>();
  public static List<String> kafkaCluster = new ArrayList<String>();
  public static String kafkaTopic;
  public static List<Integer> kafkaTopicPartitionList;
  public static List<String> kafkaEventReaderList;
  public static String kairosDBUrl;
  public static String backfillDefault = "";

  public static void getPropertiesValues(String propFileName) throws Exception {
    try {
      Properties prop = new Properties();

      InputStream inputStream = new FileInputStream(propFileName);
      prop.load(inputStream);



      // kafka configuration
      String zookepers = prop.getProperty("zookeepers");
      if (!Constant.EMPTY_STRING.equals(zookepers)) {
        String[] array = zookepers.split(",");
        for (int i = 0; i < array.length; i++) {
          listZookeeper.add(array[i].trim());
        }
      }
      String kafkaNodes = prop.getProperty("kafka_cluster");
      if (!Constant.EMPTY_STRING.equals(kafkaNodes)) {
        String[] array = kafkaNodes.split(",");
        for (int i = 0; i < array.length; i++) {
          kafkaCluster.add(array[i].trim());
        }
      }
      kafkaTopic = prop.getProperty("kafka.topic.name");

      kafkaTopicPartitionList = new ArrayList<Integer>();
      String kafkaTopicNumPartitionStr = prop.getProperty("kafka.topic.partition.list").trim();
      if (kafkaTopicNumPartitionStr.length() > 0) {
        String[] temp = kafkaTopicNumPartitionStr.split(",");
        for (int i = 0, length = temp.length; i < length; i++) {
          kafkaTopicPartitionList.add(Integer.valueOf(temp[i]));
        }
      }

      kafkaEventReaderList = new ArrayList<String>();
      String kafkaEventReaderListStr = prop.getProperty("kafka.event.reader.list").trim();
      if (kafkaEventReaderListStr.length() > 0) {
        String[] temp = kafkaEventReaderListStr.split(",");
        for (int i = 0, length = temp.length; i < length; i++) {
          kafkaEventReaderList.add(temp[i]);
        }
      }



      // KairosDB config
      kairosDBUrl = prop.getProperty("kafka.event.reader.list");



      // Esper config
      backfillDefault = prop.getProperty("esper.backfill.howfar.default");

    } catch (Exception e) {
      throw e;
    }
  }
}

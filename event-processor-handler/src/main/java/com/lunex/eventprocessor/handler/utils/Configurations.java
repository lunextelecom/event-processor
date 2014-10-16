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
      String kafkaTopicNumPartitionStr = prop.getProperty("kafka.topic.partition").trim();
      if (kafkaTopicNumPartitionStr.length() > 0) {
        String[] temp = kafkaTopicNumPartitionStr.split(",");
        for (int i = 0, length = temp.length; i < length; i++) {
          kafkaTopicPartitionList.add(Integer.valueOf(temp[i]));
        }
      }


      kairosDBUrl = prop.getProperty("kairosdb.url");

      backfillDefault = prop.getProperty("esper.backfill.howfar.default");

    } catch (Exception e) {
      throw e;
    }
  }
}

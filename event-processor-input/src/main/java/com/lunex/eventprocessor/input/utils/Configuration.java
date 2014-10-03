package com.lunex.eventprocessor.input.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Configuration {

  public static List<String> listZookeeper = new ArrayList<String>();
  public static List<String> kafkaCluster = new ArrayList<String>();
  public static String kafkaTopic;
  public static boolean kafkaProducerAsync = true;
  
  public static int nettyHttpServerPort = 8085;
  public static int nettyUdpServerPort = 8086;
  public static int nettyNumThread = 100;
  
  public static int timeStoreSeq = 10; // by second

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
      kafkaProducerAsync = Boolean.valueOf(prop.getProperty("kafka.producer.async"));

      // Netty configuration
      nettyHttpServerPort = Integer.valueOf(prop.getProperty("netty.http.server.port"));      
      nettyUdpServerPort = Integer.valueOf(prop.getProperty("netty.udp.server.port"));
      nettyNumThread = Integer.valueOf(prop.getProperty("netty.num.thread"));
      
    } catch (Exception e) {
      throw e;
    }
  }
}

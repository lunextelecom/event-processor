package com.lunex.eventprocessor.input.kafka;

import kafka.admin.DeleteTopicCommand;

public class KafkaCommand {
//  public static void createTopic(String zookeeper, String numReplicant, String numPartition,
//      String topicName) {
//    String[] arguments = new String[8];
//    arguments[0] = "--zookeeper";
//    arguments[1] = zookeeper;
//    arguments[2] = "--replica";
//    arguments[3] = numReplicant;
//    arguments[4] = "--partition";
//    arguments[5] = numPartition;
//    arguments[6] = "--topic";
//    arguments[7] = topicName;
//    CreateTopicCommand.main(arguments);
//  }

//  public static void listTopic(String zookeeper) {
//    String[] arguments = new String[2];
//    arguments[0] = "--zookeeper";
//    arguments[1] = zookeeper;
//    ListTopicCommand.main(arguments);
//  }
  
  public static void deleteTopic(String zookeeper, String topicName) {
    String[] arguments = new String[4];
    arguments[0] = "--zookeeper";
    arguments[1] = zookeeper;
    arguments[2] = "--topic";
    arguments[3] = topicName;
    DeleteTopicCommand.main(arguments);
  }
}

package com.lunex.eventprocessor.input.kafka;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka high level group consumer
 *
 */
public class KafkaHighLevelConsumerGroup {

  static final Logger logger = LoggerFactory.getLogger(KafkaHighLevelConsumerGroup.class);

  private final ConsumerConnector consumer;
  private ExecutorService executor;

  /**
   * Contructor
   * 
   * @param zookeeper
   * @param groupId
   */
  public KafkaHighLevelConsumerGroup(String zookeeper, String groupId, int numThreads) {
    ConsumerConfig config = this.createConsumerConfig(zookeeper, groupId);
    this.consumer = Consumer.createJavaConsumerConnector(config);
  }

  /**
   * Shutdown
   */
  public void shutdown() {
    if (consumer != null) {
      consumer.shutdown();
    }
    if (executor != null) {
      executor.shutdown();
    }
  }

  /**
   * Read message from topic
   * 
   * @param topicName
   * @param numThreads : equal quantity of partition of topic
   */
  public void readMessage(String topicName, int numThreads) {
    Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
    topicCountMap.put(topicName, new Integer(numThreads));
    Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap =
        consumer.createMessageStreams(topicCountMap);
    List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topicName);

    // now launch all the threads
    executor = Executors.newFixedThreadPool(numThreads);

    // now create an object to consume the messages
    //
    int threadNumber = 0;
    for (final KafkaStream<byte[], byte[]> stream : streams) {
      executor.submit(new KafkaComsumerProcessorThread(stream, threadNumber));
      threadNumber++;
    }
  }

  /**
   * Create consumer config
   * 
   * @param zookeeper
   * @param groupId
   * @return
   */
  private ConsumerConfig createConsumerConfig(String zookeeper, String groupId) {
    Properties props = new Properties();
    props.put("zookeeper.connect", zookeeper);
    props.put("group.id", groupId);
    props.put("zookeeper.session.timeout.ms", "40000");
    props.put("zookeeper.sync.time.ms", "200");
    props.put("auto.commit.interval.ms", "1000");

    return new ConsumerConfig(props);

  }

  public static void main(String[] args) {
    String zooKeeper = "192.168.93.38:2181";
    String groupId = "testKafkaGroup";
    String topic = "testKafka";
    int numThreads = 5;

    KafkaHighLevelConsumerGroup example =
        new KafkaHighLevelConsumerGroup(zooKeeper, groupId, numThreads);
    System.out.println("Connected");
    example.readMessage(topic, numThreads);
    System.out.println("Checked");
    try {
      Thread.sleep(100);
    } catch (InterruptedException ie) {

    }
    // example.shutdown();
  }
}

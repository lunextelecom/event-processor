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

public class KafkaConsumerGroup {
  private final ConsumerConnector consumer;
  private final String topic;
  private ExecutorService executor;

  public KafkaConsumerGroup(String zookeeper, String groupId, String topicName) {
    ConsumerConfig config = this.createConsumerConfig(zookeeper, groupId);
    consumer = Consumer.createJavaConsumerConnector(config);
    this.topic = topicName;
  }

  public void shutdown() {
    if (consumer != null) {
      consumer.shutdown();
    }
    if (executor != null) {
      executor.shutdown();
    }
  }

  public void run(int numThreads) {
    Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
    topicCountMap.put(topic, new Integer(numThreads));
    Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap =
        consumer.createMessageStreams(topicCountMap);
    List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);

    // now launch all the threads
    //
    executor = Executors.newFixedThreadPool(numThreads);

    // now create an object to consume the messages
    //
    int threadNumber = 0;
    for (final KafkaStream<byte[], byte[]> stream : streams) {
      executor.submit(new KafkaComsumerProcessorThread(stream, threadNumber));
      threadNumber++;
    }
  }

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
    int threads = 5;

    KafkaConsumerGroup example = new KafkaConsumerGroup(zooKeeper, groupId, topic);
    System.out.println("Connected");
    while (true) {
      example.run(threads);
      System.out.println("Checked");
      try {
        Thread.sleep(100);
      } catch (InterruptedException ie) {

      }
    }
//     example.shutdown();
  }
}

package com.lunex.eventprocessor.input.kafka;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.cluster.Broker;
import kafka.common.ErrorMapping;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaSimpleConsumer {

  private List<String> replicaBrokersList = new ArrayList<String>();

  public static void main(String args[]) {
    KafkaSimpleConsumer example = new KafkaSimpleConsumer();
    long maxReads = Long.parseLong("1");
    String topic = "testKafka";
    int partition = Integer.parseInt("3");
    List<String> seeds = new ArrayList<String>();
    seeds.add("192.168.93.38");
    seeds.add("192.168.93.39");
    int port = Integer.parseInt("9092");
    try {
      example.run(maxReads, topic, partition, seeds, port);
    } catch (Exception e) {
      System.out.println("Oops:" + e);
      e.printStackTrace();
    }
  }

  public KafkaSimpleConsumer() {
    replicaBrokersList = new ArrayList<String>();
  }

  public void run(long maxReads, String topicName, int partitionIndex, List<String> listBrokers,
      int port) throws Exception {
    // find the meta data about the topic and partition we are interested in
    PartitionMetadata metadata = this.findLeader(listBrokers, port, topicName, partitionIndex);
    if (metadata == null) {
      System.out.println("Can't find metadata for Topic and Partition. Exiting");
      return;
    }
    // check leader from metadata
    if (metadata.leader() == null) {
      System.out.println("Can't find Leader for Topic and Partition. Exiting");
      return;
    }
    // get leader Broker
    String leadHost = metadata.leader().host();
    String clientName = "Client_" + topicName + "_" + partitionIndex;

    // create Simple consumer
    SimpleConsumer consumer = new SimpleConsumer(leadHost, port, 100000, 64 * 1024, clientName);
    long readOffset =
        this.getLastOffset(consumer, topicName, partitionIndex,
            kafka.api.OffsetRequest.EarliestTime(), clientName);

    int numErrors = 0;
    while (maxReads > 0) {
      if (consumer == null) {// create consumer again from new other leader if fetchResponse error
        consumer = new SimpleConsumer(leadHost, port, 100000, 64 * 1024, clientName);
      }
      FetchRequest req =
          new FetchRequestBuilder().clientId(clientName)
              .addFetch(topicName, partitionIndex, readOffset, 100000) // Note: this fetchSize of
                                                                       // 100000
              // might need to be increased if
              // large batches are written to
              // Kafka
              .build();
      FetchResponse fetchResponse = consumer.fetch(req);

      if (fetchResponse.hasError()) {
        numErrors++;
        // Something went wrong!
        short code = fetchResponse.errorCode(topicName, partitionIndex);
        System.out.println("Error fetching data from the Broker:" + leadHost + " Reason: " + code);
        if (numErrors > 5)
          break;
        if (code == ErrorMapping.OffsetOutOfRangeCode()) {
          // We asked for an invalid offset. For simple case ask for the last element to reset
          readOffset =
              getLastOffset(consumer, topicName, partitionIndex,
                  kafka.api.OffsetRequest.LatestTime(), clientName);
          continue;
        }
        consumer.close();
        consumer = null;
        leadHost = this.findNewLeader(leadHost, topicName, partitionIndex, port);
        continue;
      }
      numErrors = 0;

      long numRead = 0;
      for (MessageAndOffset messageAndOffset : fetchResponse.messageSet(topicName, partitionIndex)) {
        long currentOffset = messageAndOffset.offset();
        if (currentOffset < readOffset) {
          System.out.println("Found an old offset: " + currentOffset + " Expecting: " + readOffset);
          continue;
        }
        readOffset = messageAndOffset.nextOffset();
        ByteBuffer payload = messageAndOffset.message().payload();

        byte[] bytes = new byte[payload.limit()];
        payload.get(bytes);
        String content = new String(bytes, "UTF-8");
        System.out.println(String.valueOf(messageAndOffset.offset()) + ": " + content);
        numRead++;
        maxReads--;
      }

      if (numRead == 0) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
        }
      }
    }
    if (consumer != null) {
      consumer.close();
    }
  }

  public long getLastOffset(SimpleConsumer consumer, String topic, int partition, long whichTime,
      String clientName) {
    TopicAndPartition topicAndPartition = new TopicAndPartition(topic, partition);
    Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo =
        new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
    requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));
    OffsetRequest request =
        new OffsetRequest(requestInfo, kafka.api.OffsetRequest.CurrentVersion(), clientName);
    OffsetResponse response = consumer.getOffsetsBefore(request);

    if (response.hasError()) {
      System.out.println("Error fetching data Offset Data the Broker. Reason: "
          + response.errorCode(topic, partition));
      return 0;
    }
    long[] offsets = response.offsets(topic, partition);
    return offsets[0];
  }

  private String findNewLeader(String oldLeader, String topicName, int partitionIndex, int port)
      throws Exception {
    for (int i = 0; i < 3; i++) {
      boolean goToSleep = false;
      PartitionMetadata metadata =
          this.findLeader(replicaBrokersList, port, topicName, partitionIndex);
      if (metadata == null) {
        goToSleep = true;
      } else if (metadata.leader() == null) {
        goToSleep = true;
      } else if (oldLeader.equalsIgnoreCase(metadata.leader().host()) && i == 0) {
        // first time through if the leader hasn't changed give ZooKeeper a second to recover
        // second time, assume the broker did recover before failover, or it was a non-Broker issue
        //
        goToSleep = true;
      } else {
        return metadata.leader().host();
      }
      if (goToSleep) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
        }
      }
    }
    System.out.println("Unable to find new leader after Broker failure. Exiting");
    throw new Exception("Unable to find new leader after Broker failure. Exiting");
  }

  private PartitionMetadata findLeader(List<String> listBroker, int port, String topicName,
      int partitionIndex) {
    PartitionMetadata returnMetaData = null;
    loop: for (String seed : listBroker) {
      SimpleConsumer consumer = null;
      try {
        consumer = new SimpleConsumer(seed, port, 100000, 64 * 1024, "leaderLookup");
        List<String> topics = Collections.singletonList(topicName);
        TopicMetadataRequest req = new TopicMetadataRequest(topics);
        kafka.javaapi.TopicMetadataResponse resp = consumer.send(req);

        List<TopicMetadata> metaData = resp.topicsMetadata();
        for (TopicMetadata item : metaData) {
          for (PartitionMetadata part : item.partitionsMetadata()) {
            if (part.partitionId() == partitionIndex) {
              returnMetaData = part;
              break loop;
            }
          }
        }
      } catch (Exception e) {
        System.out.println("Error communicating with Broker [" + seed + "] to find Leader for ["
            + topicName + ", " + partitionIndex + "] Reason: " + e);
      } finally {
        if (consumer != null)
          consumer.close();
      }
    }
    if (returnMetaData != null) {
      replicaBrokersList.clear();
      for (Broker replica : returnMetaData.replicas()) {
        replicaBrokersList.add(replica.host());
      }
    }
    return returnMetaData;
  }
}

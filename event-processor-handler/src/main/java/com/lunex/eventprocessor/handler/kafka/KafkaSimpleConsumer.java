package com.lunex.eventprocessor.handler.kafka;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.cluster.Broker;
import kafka.common.ErrorMapping;
import kafka.common.TopicAndPartition;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.Message;
import kafka.message.MessageAndOffset;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka simple consumer
 *
 */
public class KafkaSimpleConsumer {

  static final Logger logger = LoggerFactory.getLogger(KafkaSimpleConsumer.class);

  private List<String> replicaBrokersList = new ArrayList<String>();
  private String topicName;
  private int partitionIndex;
  private int maxReads = -1; // -1 -> no limit
  private int maxError = 5;
  private KafkaMessageProcessor processorMessage;
  public boolean stoped = false;
  public long readOffset = -1;

  public int getMaxError() {
    return maxError;
  }

  public void setMaxError(int maxError) {
    this.maxError = maxError;
  }

  /**
   * Contructor
   * 
   * @param listBroker : list broker of kafka (192.168.3.3:9092, 192.168.3.4:9092, ...)
   * @param topicName : topic name where message is sent to
   * @param partitionIndex : index of partition of topic, where consumer need to read message
   * @param maxReads: -1 is unlimit > -1 is limit to read message from partion of topic
   */
  public KafkaSimpleConsumer(List<String> listBroker, String topicName, int partitionIndex,
      int maxReads, KafkaMessageProcessor processorMessage) {
    this.replicaBrokersList = new ArrayList<String>(listBroker);
    this.topicName = topicName;
    this.partitionIndex = partitionIndex;
    this.maxReads = maxReads;
    this.processorMessage = processorMessage;
  }

  /**
   * Read message from Kafka
   * 
   * @param maxReads : max number message need to read
   * @param topicName : Name of topic
   * @param partitionIndex: index of partition
   * @param listBrokers: list broker
   * @param port: port to read
   * @param whichTime : determine offset need to get message
   * @throws Exception
   */
  public void readKafka(long whichTime) throws Exception {
    // find the meta data about the topic and partition we are interested in
    PartitionMetadata metadata =
        this.findLeader(this.replicaBrokersList, topicName, partitionIndex);
    if (metadata == null) {
      logger.info("Can't find metadata for Topic and Partition. Exiting");
      return;
    }
    // check leader from metadata
    if (metadata.leader() == null) {
      logger.info("Can't find Leader for Topic and Partition. Exiting");
      return;
    }
    // get leader Broker
    String leadHost = metadata.leader().host();
    int port = metadata.leader().port();
    String clientName = "Client_" + topicName + "_" + partitionIndex;

    // create Simple consumer
    SimpleConsumer consumer = new SimpleConsumer(leadHost, port, 100000, 64 * 1024, clientName);
    if (readOffset == -1) {
      readOffset = this.getLastOffset(consumer, topicName, partitionIndex, whichTime, clientName);
    }

    int numErrors = 0;
    boolean unlimit = maxReads == -1;
    while (maxReads > 0 || unlimit) {
      if (stoped == true) {
        break;
      }
      if (consumer == null) {// create consumer again from new other leader if fetchResponse error
        consumer = new SimpleConsumer(leadHost, port, 100000, 64 * 1024, clientName);
      }
      // Note: this fetchSize of 100000 might need to be increased if large batches are written to
      // Kafka
      FetchRequest req =
          new FetchRequestBuilder().clientId(clientName)
              .addFetch(topicName, partitionIndex, readOffset, 100000).build();
      FetchResponse fetchResponse = consumer.fetch(req);

      if (fetchResponse.hasError()) {
        numErrors++;
        // Something went wrong!
        short code = fetchResponse.errorCode(topicName, partitionIndex);
        logger.error("Error fetching data from the Broker:" + leadHost + " Reason: " + code);
        if (numErrors > 5)
          break;
        if (code == ErrorMapping.OffsetOutOfRangeCode()) {
          // We asked for an invalid offset. For simple case ask for the last element to reset
          readOffset = getLastOffset(consumer, topicName, partitionIndex, whichTime, clientName);
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
          // System.out.println("Found an old offset: " + currentOffset + " Expecting: " +
          // readOffset);
          logger.info("Found an old offset: " + currentOffset + " Expecting: " + readOffset);
          continue;
        }
        readOffset = messageAndOffset.nextOffset();

        // get message
        Message message = messageAndOffset.message();
        // get content of payload from message
        ByteBuffer payload = message.payload();
        byte[] bytes = new byte[payload.limit()];
        payload.get(bytes);
        String content = new String(bytes, "UTF-8");
        // System.out.println(String.valueOf(messageAndOffset.offset()) + ": " + content);
        logger.info(String.valueOf(messageAndOffset.offset()) + ": " + content);

        // TODO something with message
        if (this.processorMessage != null) {
          this.processorMessage.processMessage(bytes);
        }

        numRead++;
        maxReads--;
      }

      // If no anything to read from kafka
      if (numRead == 0) {
        try {
          Thread.sleep(1000); // waiting 1000ms before read again
        } catch (InterruptedException ie) {
          logger.error(ie.getMessage(), ie);
        }
      }
    }
    if (consumer != null) {
      consumer.close();
    }
  }

  /**
   * Get last of set by time
   * 
   * @param consumer
   * @param topic
   * @param partitionIndex
   * @param whichTime
   * @param clientName
   * @return
   */
  public long getLastOffset(SimpleConsumer consumer, String topic, int partitionIndex,
      long whichTime, String clientName) {
    TopicAndPartition topicAndPartition = new TopicAndPartition(topic, partitionIndex);
    Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo =
        new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
    requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(whichTime, 1));
    OffsetRequest request =
        new OffsetRequest(requestInfo, kafka.api.OffsetRequest.CurrentVersion(), clientName);
    OffsetResponse response = consumer.getOffsetsBefore(request);

    if (response.hasError()) {
      logger.info("Error fetching data Offset Data the Broker. Reason: "
          + response.errorCode(topic, partitionIndex));
      return 0;
    }
    long[] offsets = response.offsets(topic, partitionIndex);
    return offsets[0];
  }

  /**
   * Find new leader of partion
   * 
   * @param oldLeader
   * @param topicName
   * @param partitionIndex
   * @param port
   * @return
   * @throws Exception
   */
  private String findNewLeader(String oldLeader, String topicName, int partitionIndex, int port)
      throws Exception {
    for (int i = 0; i < 3; i++) {
      boolean goToSleep = false;
      PartitionMetadata metadata = this.findLeader(replicaBrokersList, topicName, partitionIndex);
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
    logger.error("Unable to find new leader after Broker failure. Exiting");
    throw new Exception("Unable to find new leader after Broker failure. Exiting");
  }

  /**
   * Find leader of partion
   * 
   * @param listBroker
   * @param topicName
   * @param partitionIndex
   * @return
   */
  private PartitionMetadata findLeader(List<String> originallistBroker, String topicName,
      int partitionIndex) {
    List<String> listBroker = new ArrayList<String>(originallistBroker);
    PartitionMetadata returnMetaData = null;
    loop: for (String seed : listBroker) {
      String[] temp = seed.split(":");
      SimpleConsumer consumer = null;
      try {
        consumer =
            new SimpleConsumer(temp[0], Integer.valueOf(temp[1]), 100000, 64 * 1024, "leaderLookup");
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
        logger.error("Error communicating with Broker [" + seed + "] to find Leader for ["
            + topicName + ", " + partitionIndex + "] Reason: " + e, e);
      } finally {
        if (consumer != null)
          consumer.close();
      }
    }
    if (returnMetaData != null) {
      replicaBrokersList.clear();
      for (Broker replica : returnMetaData.replicas()) {
        replicaBrokersList.add(replica.host() + ":" + replica.port());
      }
    }
    return returnMetaData;
  }

  public static void main(String args[]) {
    List<String> listBrokers = new ArrayList<String>();
    listBrokers.add("192.168.93.38:9092");
    listBrokers.add("192.168.93.39:9092");
    KafkaSimpleConsumer example =
        new KafkaSimpleConsumer(listBrokers, "testKafka", 0, -1, new KafkaMessageProcessor() {

          public Object processMessage(byte[] message) {
            try {
              int contentType = (int) message[0];
              switch (contentType) {
                case 1:// JSON
                  System.out.println("JSon");
                  break;
                default:
                  break;
              }
              message = Arrays.copyOfRange(message, 1, message.length);
              System.out.println("Message content: " + new String(message, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            }
            return null;
          }
        });

    try {
      example.readKafka(kafka.api.OffsetRequest.LatestTime());
    } catch (Exception e) {
      System.out.println("Oops:" + e);
      e.printStackTrace();
    }
  }
}

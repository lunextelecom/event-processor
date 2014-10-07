package com.lunex.eventprocessor.input;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import kafka.serializer.StringEncoder;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.input.kafka.ASCIIPartitioner;
import com.lunex.eventprocessor.input.kafka.KafkaProducer;
import com.lunex.eventprocessor.input.netty.NettyHttpSnoopServer;
import com.lunex.eventprocessor.input.netty.NettyUDPServer;
import com.lunex.eventprocessor.input.utils.Configuration;
import com.lunex.eventprocessor.input.utils.SeqTimerTask;

/**
 * Hello world!
 *
 */
public class App {

  static final Logger logger = LoggerFactory.getLogger(App.class);
  private static NettyHttpSnoopServer httpServer;
  private static NettyUDPServer udpServer;
  public static SeqTimerTask seqTimerTask;
  public static KafkaProducer kafkaProducer;

  public static void main(String[] args) {
    logger.info("Hello event processor!");
    try {
      // load log properties
      Properties props = new Properties();
      props.load(new FileInputStream("src/main/resources/log4j.properties"));
      PropertyConfigurator.configure(props);

      // Load config
      Configuration.getPropertiesValues("src/main/resources/app.properties");

      // start netty server
      App.startHttpServer();
      App.startUDPServer();

      // seqTimerTask
      App.seqTimerTask = new SeqTimerTask();
      App.seqTimerTask.start(Configuration.timeIntervalCheckSeq);

      // create kafka producer
      kafkaProducer =
          new KafkaProducer(Configuration.kafkaCluster, StringEncoder.class.getName(),
              ASCIIPartitioner.class.getName(), Configuration.kafkaProducerAsync);
    } catch (IOException ex) {
      logger.error(ex.getMessage());
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }

  /**
   * Start netty server as HTTP server
   * 
   */
  public static void startHttpServer() {
    httpServer = new NettyHttpSnoopServer(Configuration.nettyHttpServerPort);
    Thread thread = new Thread(new Runnable() {
      public void run() {
        try {
          httpServer.startServer();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    thread.start();
  }

  /**
   * Start netty server as UDP server
   */
  public static void startUDPServer() {
    udpServer = new NettyUDPServer(Configuration.nettyUdpServerPort);
    Thread thread = new Thread(new Runnable() {
      public void run() {
        try {
          udpServer.startServer();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    thread.start();
  }
}

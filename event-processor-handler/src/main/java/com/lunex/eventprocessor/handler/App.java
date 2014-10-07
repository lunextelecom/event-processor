package com.lunex.eventprocessor.handler;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.lunex.eventprocessor.handler.continuousquery.EventProcessorHandler;
import com.lunex.eventprocessor.handler.kafka.IKafkaMessageProcessor;
import com.lunex.eventprocessor.handler.kafka.KafkaSimpleConsumer;
import com.lunex.eventprocessor.handler.utils.Configuration;

/**
 * Hello world!
 *
 */
public class App {

  public static EventProcessorHandler eventHandler;

  public static void main(String[] args) {
    System.out.println("Hello World!");
    try {
      // load log properties
      Properties props = new Properties();
      props.load(new FileInputStream("src/main/resources/log4j.properties"));
      PropertyConfigurator.configure(props);

      // Load config
      Configuration.getPropertiesValues("src/main/resources/app.properties");

      eventHandler = new EventProcessorHandler();
      eventHandler.loadEPLRuntime("eventProcessor");
    } catch (Exception ex) {
    }
  }
}

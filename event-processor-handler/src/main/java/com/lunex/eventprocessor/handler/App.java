package com.lunex.eventprocessor.handler;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.handler.reader.ConsoleOutput;
import com.lunex.eventprocessor.handler.reader.EsperProcessor;
import com.lunex.eventprocessor.handler.reader.EventReader;
import com.lunex.eventprocessor.handler.reader.Processor;
import com.lunex.eventprocessor.handler.reader.KafkaReader;
import com.lunex.eventprocessor.handler.reader.QueryHierarchy;
import com.lunex.eventprocessor.handler.utils.Configuration;

/**
 * Hello world!
 *
 */
public class App {

  public static void main(String[] args) {
    System.out.println("Hello World!");
    try {
      // load log properties
      Properties props = new Properties();
      props.load(new FileInputStream("src/main/resources/log4j.properties"));
      PropertyConfigurator.configure(props);

      // Load config
      Configuration.getPropertiesValues("src/main/resources/app.properties");

      EventReader reader = new KafkaReader();
      Processor processor = new EsperProcessor();
      EventQuery query = new EventQuery();
      QueryHierarchy hierarchy = new QueryHierarchy();
      hierarchy.addQuery("new_order", query, new ResultListener[] {new ConsoleOutput()});
      processor.setHierarchy(hierarchy);
      reader.read(processor);
    } catch (Exception ex) {
    }
  }
}

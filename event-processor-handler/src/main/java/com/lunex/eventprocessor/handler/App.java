package com.lunex.eventprocessor.handler;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

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

    } catch (Exception ex) {
    }
  }
}

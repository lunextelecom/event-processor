package com.lunex.eventprocessor.handler;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryHierarchy;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.dataaccess.KairosDBClient;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.handler.listener.CassandraWriter;
import com.lunex.eventprocessor.handler.listener.ConsoleOutput;
import com.lunex.eventprocessor.handler.listener.KairosDBWriter;
import com.lunex.eventprocessor.handler.processor.EsperProcessor;
import com.lunex.eventprocessor.handler.processor.KairosDBProcessor;
import com.lunex.eventprocessor.handler.processor.Processor;
import com.lunex.eventprocessor.handler.reader.EventReader;
import com.lunex.eventprocessor.handler.reader.KafkaReader;
import com.lunex.eventprocessor.handler.utils.Configurations;

/**
 * Setup KafkaReader Setup EsperProcessor
 */
public class App extends Application<WebConfiguration> {

  static final Logger logger = LoggerFactory.getLogger(App.class);

  public static List<EventProperty> listEventProperty;
  public static QueryHierarchy hierarchy;
  public static KairosDBClient kairosDB;
  public static Processor esperProcessor;
  public static Processor kairosDBProcessor;
  public static EventReader readerEsperProcessor;
  public static EventReader readerKairosDBProcessor;

  public static void main(String[] args) {
    System.out.println("Hello World!");
    try {
      // load log properties
      Properties props = new Properties();
      props.load(new FileInputStream("src/main/resources/log4j.properties"));
      PropertyConfigurator.configure(props);

      // Load config
      Configurations.getPropertiesValues("src/main/resources/app.properties");

      // kairosdb
      kairosDB = new KairosDBClient(Configurations.kairosDBUrl);

      // get EventQuery
      List<EventQuery> listEventQuery =
          CassandraRepository.getInstance().getEventQueryFromDB(-1, "", "");
      List<List<EventQuery>> grouping =
          EventQueryProcessor.groupEventQueryByEventName(listEventQuery);
      // get Eventproperties
      listEventProperty = EventQueryProcessor.processEventProperyForEventQuery(listEventQuery);

      // Create QueryHierarchy
      hierarchy = new QueryHierarchy();
      for (int i = 0; i < grouping.size(); i++) {
        List<EventQuery> subList = grouping.get(i);
        for (int j = 0; j < subList.size(); j++) {
          EventQuery query = subList.get(j);
          hierarchy.addQuery(query.getEventName(), query, new ResultListener[] {
              new ConsoleOutput()/*, new KairosDBWriter(), new CassandraWriter()*/});
        }
      }

      // create esper processor
      esperProcessor =
          new EsperProcessor(hierarchy, listEventProperty, listEventQuery, true,
              StringUtils.getBackFillTime(Configurations.backfillDefault));
      // create event reader
      readerEsperProcessor = new KafkaReader();
      // reader read event and send to processor
      Thread esper = new Thread(new Runnable() {
        public void run() {
          readerEsperProcessor.read(esperProcessor);
        }
      });
      esper.start();

      // create kairos processor
      kairosDBProcessor = new KairosDBProcessor();
      kairosDBProcessor.setHierarchy(hierarchy);
      // read event and send to processor
      readerKairosDBProcessor = new KafkaReader();
      Thread kairos = new Thread(new Runnable() {
        public void run() {
          readerKairosDBProcessor.read(kairosDBProcessor);
        }
      });
      kairos.start();


      // start rest api service
      String[] temp = new String[] {"server", "src/main/resources/config.yaml"};
      new App().run(temp);
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
    }
  }

  @Override
  public void initialize(Bootstrap<WebConfiguration> bootstrap) {
    // TODO Auto-generated method stub
  }

  @Override
  public void run(WebConfiguration configuration, Environment environment) throws Exception {
    final EventHandlerApiServiceResource serviceResource = new EventHandlerApiServiceResource();
    environment.jersey().register(serviceResource);
  }
}

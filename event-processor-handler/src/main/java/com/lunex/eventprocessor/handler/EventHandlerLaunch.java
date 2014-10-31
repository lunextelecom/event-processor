package com.lunex.eventprocessor.handler;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import kafka.serializer.StringEncoder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryHierarchy;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.dataaccess.KairosDBClient;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.handler.kafka.ASCIIPartitioner;
import com.lunex.eventprocessor.handler.kafka.KafkaProducer;
import com.lunex.eventprocessor.handler.listener.CassandraWriter;
import com.lunex.eventprocessor.handler.listener.ConsoleOutput;
import com.lunex.eventprocessor.handler.listener.KafkaWriter;
import com.lunex.eventprocessor.handler.listener.KairosDBWriter;
import com.lunex.eventprocessor.handler.processor.EsperProcessor;
import com.lunex.eventprocessor.handler.processor.Processor;
import com.lunex.eventprocessor.handler.reader.EventReader;
import com.lunex.eventprocessor.handler.reader.KafkaReader;
import com.lunex.eventprocessor.handler.rest.EventHandlerApiServiceResource;
import com.lunex.eventprocessor.handler.rest.WebConfiguration;
import com.lunex.eventprocessor.handler.utils.Configurations;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReaders;

/**
 * Setup KafkaReader Setup EsperProcessor
 */
public class EventHandlerLaunch extends Application<WebConfiguration> {

  static final Logger logger = LoggerFactory.getLogger(EventHandlerLaunch.class);

  public static List<EventProperty> listEventProperty;
  public static QueryHierarchy hierarchy;
  public static KairosDBClient kairosDB;
  public static Processor esperProcessor;
  public static KafkaProducer kafkaProducer;
  public static EventReader readerEsperProcessor;

  private static final String OPTION_APP = "a";
  private static final String OPTION_HELP = "h";
  private static final String OPTION_CONF = "c";

  public static void main(String[] args) {
    try {

      final Options options = new Options();
      options.addOption(null, OPTION_APP, true, "app.properties");
      options.addOption(null, OPTION_CONF, true, "config.yaml");
      options.addOption(null, OPTION_HELP, false, "Display command line help.");
      final CommandLineParser parser = new PosixParser();
      final CommandLine cmd;
      try {
        cmd = parser.parse(options, args);
        if (cmd.getArgs().length > 0) {
          throw new UnrecognizedOptionException("Extra arguments were provided in "
              + Arrays.asList(args));
        }
      } catch (final ParseException e) {
        printHelp(options, "Could not parse command line: " + Arrays.asList(args));
        return;
      }
      if (cmd.hasOption(OPTION_HELP) || !cmd.hasOption(OPTION_APP) || !cmd.hasOption(OPTION_CONF)) {
        printHelp(options, null);
        return;
      }

      // load log properties
      // Properties props = new Properties();
      // props.load(new FileInputStream("conf/log4j.properties"));
      // PropertyConfigurator.configure(props);

      // Load config
      String appConfig = cmd.getOptionValue(OPTION_APP);
      Configurations.getPropertiesValues(appConfig);

      // kairosdb
      kairosDB = new KairosDBClient(Configurations.kairosDBUrl);

      // get EventQuery
      hierarchy = new QueryHierarchy();
      List<EventQuery> listEventQuery =
          CassandraRepository.getInstance(Configurations.cassandraHost,
              Configurations.cassandraKeyspace).getEventQueryFromDB("", "");
      if (listEventQuery != null && !listEventQuery.isEmpty()) {
        List<List<EventQuery>> grouping =
            EventQueryProcessor.groupEventQueryByEventName(listEventQuery);
        // get Eventproperties
        listEventProperty = EventQueryProcessor.processEventProperyForEventQuery(listEventQuery);

        // Create QueryHierarchy
        for (int i = 0; i < grouping.size(); i++) {
          List<EventQuery> subList = grouping.get(i);
          for (int j = 0; j < subList.size(); j++) {
            EventQuery query = subList.get(j);
            if (Configurations.ruleList != null && !Configurations.ruleList.isEmpty()
                && !Configurations.ruleList.contains(query.getRuleName())) {
              continue;
            }
            hierarchy.addQuery(query.getEventName(), query,
                new ResultListener[] {new ConsoleOutput(), new CassandraWriter(),
                    new KairosDBWriter(), new KafkaWriter()});
          }
        }
      } else {
        logger.info("No rule in DB, please check again!");
      }

      // create esper processor
      esperProcessor =
          new EsperProcessor(hierarchy, listEventProperty, listEventQuery,
              Configurations.esperBackfill,
              StringUtils.getBackFillTime(Configurations.esperBackfillDefault));
      // create event reader
      if (Strings.isNullOrEmpty(Configurations.kafkaBackRead.trim())) {
        readerEsperProcessor = new KafkaReader();
      } else {
        readerEsperProcessor =
            new KafkaReader(Configurations.kafkaBackRead.trim(), "dd-MM-yyyy HH:mm:ss z");
      }
      // reader read event and send to processor
      Thread esper = new Thread(new Runnable() {
        public void run() {
          readerEsperProcessor.read(esperProcessor);
        }
      });
      esper.start();

      // Init kafka producer
      kafkaProducer =
          new KafkaProducer(Configurations.kafkaCluster, StringEncoder.class.getName(),
              ASCIIPartitioner.class.getName(), true);

      // start rest api service
      String restConfig = cmd.getOptionValue(OPTION_CONF);
      String[] temp = new String[] {"server", restConfig};
      new EventHandlerLaunch().run(temp);
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
    }
  }

  private static void printHelp(final Options options, final String errorMessage) {
    if (!Strings.isNullOrEmpty(errorMessage)) {
      System.err.println(errorMessage);
    }
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("event-processor-handler", options);
  }

  @Override
  public void initialize(Bootstrap<WebConfiguration> bootstrap) {
    // TODO Auto-generated method stub
  }

  @Override
  public void run(WebConfiguration configuration, Environment environment) throws Exception {
    final EventHandlerApiServiceResource serviceResource = new EventHandlerApiServiceResource();

    // Config CORS
    FilterRegistration.Dynamic filter =
        environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    filter.setInitParameter("allowedOrigins", "*"); // allowed origins comma separated
    filter.setInitParameter("allowedHeaders",
        "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
    filter.setInitParameter("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS,HEAD");
    filter.setInitParameter("preflightMaxAge", "5184000"); // 2 months
    filter.setInitParameter("allowCredentials", "true");
    // config for swagger and add resource webservice for dropwizad
    environment.jersey().register(new ApiListingResourceJSON());
    environment.jersey().register(serviceResource);
    environment.jersey().register(serviceResource);
    environment.jersey().register(new ResourceListingProvider());
    environment.jersey().register(new ApiDeclarationProvider());
    ScannerFactory.setScanner(new DefaultJaxrsScanner());
    ClassReaders.setReader(new DefaultJaxrsApiReader());
    SwaggerConfig config = ConfigFactory.config();
    config.setApiVersion("1.0.0");
    config.setBasePath("/");
  }
}

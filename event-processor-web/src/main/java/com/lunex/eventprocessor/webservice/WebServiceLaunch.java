package com.lunex.eventprocessor.webservice;

import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.webservice.rest.EventProcessorWebServiceResource;
import com.lunex.eventprocessor.webservice.rest.WebConfiguration;
import com.lunex.eventprocessor.webservice.service.EventProcessorService;
import com.lunex.eventprocessor.webservice.service.EventProcessorServiceAdmin;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Hello world!
 *
 */
public class WebServiceLaunch extends Application<WebConfiguration> {

  static final Logger logger = LoggerFactory.getLogger(WebServiceLaunch.class);

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
      if (cmd.hasOption(OPTION_HELP) || !cmd.hasOption(OPTION_CONF)) {
        printHelp(options, null);
        return;
      }

      // start rest api service
      String restConfig = cmd.getOptionValue(OPTION_CONF);
      String[] temp = new String[] {"server", restConfig};
      new WebServiceLaunch().run(temp);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
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
    EventProcessorService service =
        configuration.getCcServiceFactory().buildEventProcessorService(environment);

    EventProcessorServiceAdmin serviceAdmin =
        configuration.getCcServiceFactory().buildEventProcessorServiceAdmin(environment);

    final EventProcessorWebServiceResource serviceResource =
        new EventProcessorWebServiceResource(service);
    environment.jersey().register(serviceResource);
  }
}

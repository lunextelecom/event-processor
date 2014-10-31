package com.lunex.eventprocessor.webservice;

import java.util.Arrays;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.lunex.eventprocessor.webservice.rest.EventProcessorWebServiceAdminResource;
import com.lunex.eventprocessor.webservice.rest.EventProcessorWebServiceFactory;
import com.lunex.eventprocessor.webservice.rest.EventProcessorWebServiceResource;
import com.lunex.eventprocessor.webservice.rest.WebConfiguration;
import com.lunex.eventprocessor.webservice.service.EventProcessorService;
import com.lunex.eventprocessor.webservice.service.EventProcessorServiceAdmin;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReaders;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class WebServiceLaunch extends Application<WebConfiguration> {

  static final Logger logger = LoggerFactory.getLogger(WebServiceLaunch.class);

  // private static final String OPTION_APP = "a";
  private static final String OPTION_HELP = "h";
  private static final String OPTION_CONF = "c";

  public static void main(String[] args) {
    try {
      final Options options = new Options();
      // options.addOption(null, OPTION_APP, true, "app.properties");
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

  /**
   * Print help command line
   * 
   * @param options
   * @param errorMessage
   */
  private static void printHelp(final Options options, final String errorMessage) {
    if (!Strings.isNullOrEmpty(errorMessage)) {
      System.err.println(errorMessage);
    }
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("event-processor-web", options);
  }

  @Override
  public void initialize(Bootstrap<WebConfiguration> bootstrap) {
    // TODO Auto-generated method stub

  }

  @Override
  public void run(WebConfiguration configuration, Environment environment) throws Exception {
    EventProcessorWebServiceFactory factory = configuration.getCcServiceFactory();

    EventProcessorService service = factory.buildEventProcessorService(environment);
    EventProcessorServiceAdmin serviceAdmin = factory.buildEventProcessorServiceAdmin(environment);

    final EventProcessorWebServiceResource serviceResource =
        new EventProcessorWebServiceResource(service);
    final EventProcessorWebServiceAdminResource serviceAdminResource =
        new EventProcessorWebServiceAdminResource(serviceAdmin);

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
    environment.jersey().register(serviceAdminResource);
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

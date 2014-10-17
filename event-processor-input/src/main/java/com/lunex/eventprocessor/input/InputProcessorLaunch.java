package com.lunex.eventprocessor.input;

import java.io.IOException;
import java.util.Arrays;

import kafka.serializer.StringEncoder;

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
public class InputProcessorLaunch {

  static final Logger logger = LoggerFactory.getLogger(InputProcessorLaunch.class);

  private static NettyHttpSnoopServer httpServer;
  private static NettyUDPServer udpServer;
  public static SeqTimerTask seqTimerTask;
  public static KafkaProducer kafkaProducer;

  /** The admin */
  private static final String OPTION_APP = "a";
  private static final String OPTION_HELP = "h";

  public static void main(String[] args) {
    logger.info("Hello event processor!");
    try {

      final Options options = new Options();
      options.addOption(null, OPTION_APP, true, "app.properties");
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
      if (cmd.hasOption(OPTION_HELP) || !cmd.hasOption(OPTION_APP)) {
        printHelp(options, null);
        return;
      }

      // Load config
      String appConfig = cmd.getOptionValue(OPTION_APP);
      Configuration.getPropertiesValues(appConfig);

      // load log properties
      // Properties props = new Properties();
      // props.load(new FileInputStream("conf/log4j.properties"));
      // PropertyConfigurator.configure(props);

      // start netty server
      InputProcessorLaunch.startHttpServer();
      InputProcessorLaunch.startUDPServer();

      // seqTimerTask
      InputProcessorLaunch.seqTimerTask = new SeqTimerTask();
      InputProcessorLaunch.seqTimerTask.start(Configuration.timeIntervalCheckSeq);

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

  private static void printHelp(final Options options, final String errorMessage) {
    if (!Strings.isNullOrEmpty(errorMessage)) {
      System.err.println(errorMessage);
    }
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("input-processor", options);
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

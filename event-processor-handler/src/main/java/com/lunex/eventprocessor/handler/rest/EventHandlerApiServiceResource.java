package com.lunex.eventprocessor.handler.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import joptsimple.internal.Strings;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventQuery.EventQueryStatus;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.handler.EventHandlerLaunch;
import com.lunex.eventprocessor.handler.utils.Configurations;

@Path("/event-processor-handler")
public class EventHandlerApiServiceResource {

  final static org.slf4j.Logger logger = LoggerFactory
      .getLogger(EventHandlerApiServiceResource.class);

  public EventHandlerApiServiceResource() {

  }

  @GET
  @Path("/resetEPL")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ServiceResponse resetEPL() {
    return new ServiceResponse("test", true);
  }


  /**
   * Update rule when it is running
   * 
   * @param eventName
   * @param ruleName
   * @param backfill
   * @param backfillTime
   * @return
   */
  @POST
  @Path("/changerule")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ServiceResponse changeRule(@QueryParam("eventName") String eventName,
      @QueryParam("ruleName") String ruleName, @QueryParam("backfill") boolean backfill,
      @QueryParam("backfillTime") long backfillTime) {
    try {
      List<EventQuery> rules =
          CassandraRepository.getInstance(Configurations.cassandraHost, Configurations.cassandraKeyspace).getEventQueryFromDB(eventName, ruleName);
      if (rules != null && !rules.isEmpty()) {
        EventQuery rule = rules.get(0);
        EventHandlerLaunch.readerEsperProcessor.stop();
        boolean result = EventHandlerLaunch.esperProcessor.updateRule(rule, backfill, backfillTime);
        EventHandlerLaunch.readerEsperProcessor.start();
        if (result) {
          return new ServiceResponse("Change successfully", true);
        } else {
          return new ServiceResponse("Change unsuccessfully", false);
        }
      } else {
        return new ServiceResponse("No rule to process", false);
      }
    } catch (Exception e) {
      return new ServiceResponse("Change unsuccessfully", false);
    }
  }

  /**
   * Stop a rule which is running
   * 
   * @param eventName
   * @param ruleName
   * @return
   */
  @POST
  @Path("/stoprule")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ServiceResponse stopRule(@QueryParam("eventName") String eventName,
      @QueryParam("ruleName") String ruleName) {
    try {
      List<EventQuery> rules =
          CassandraRepository.getInstance(Configurations.cassandraHost, Configurations.cassandraKeyspace).getEventQueryFromDB(eventName, ruleName);
      if (rules != null && !rules.isEmpty()) {
        EventQuery rule = rules.get(0);
        if (rule.getStatus() == EventQueryStatus.STOP) {
          return new ServiceResponse("Rule is stoped", false);
        }
        EventHandlerLaunch.readerEsperProcessor.stop();
        boolean result = EventHandlerLaunch.esperProcessor.stopRule(rule);
        EventHandlerLaunch.readerEsperProcessor.start();
        if (result) {
          rule.setStatus(EventQueryStatus.STOP);
          CassandraRepository.getInstance(Configurations.cassandraHost, Configurations.cassandraKeyspace).changeEventQueryStatus(rule);
          return new ServiceResponse("Change successfully", true);
        } else {
          return new ServiceResponse("Change unsuccessfully", false);
        }
      } else {
        return new ServiceResponse("No rule to process", false);
      }
    } catch (Exception e) {
      return new ServiceResponse("Change unsuccessfully", false);
    }
  }

  /**
   * Start a stoped rule
   * 
   * @param eventName
   * @param ruleName
   * @param backfill
   * @param backfillTime
   * @return
   */
  @POST
  @Path("/startrule")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ServiceResponse startRule(@QueryParam("eventName") String eventName,
      @QueryParam("ruleName") String ruleName, @QueryParam("backfill") boolean backfill,
      @QueryParam("backfillTime") long backfillTime) {
    try {
      List<EventQuery> rules =
          CassandraRepository.getInstance(Configurations.cassandraHost, Configurations.cassandraKeyspace).getEventQueryFromDB(eventName, ruleName);
      if (rules != null && !rules.isEmpty()) {
        EventQuery rule = rules.get(0);
        if (rule.getStatus() != EventQueryStatus.STOP) {
          return new ServiceResponse("Rule is running", false);
        }
        EventHandlerLaunch.readerEsperProcessor.stop();
        boolean result =
            EventHandlerLaunch.esperProcessor.startRule(rule, backfill, backfillTime);
        EventHandlerLaunch.readerEsperProcessor.start();
        if (result) {
          rule.setStatus(EventQueryStatus.RUNNING);
          CassandraRepository.getInstance(Configurations.cassandraHost, Configurations.cassandraKeyspace).changeEventQueryStatus(rule);
          return new ServiceResponse("Change successfully", true);
        } else {
          return new ServiceResponse("Change unsuccessfully", false);
        }
      } else {
        return new ServiceResponse("No rule to process", false);
      }
    } catch (Exception e) {
      return new ServiceResponse("Change unsuccessfully", false);
    }
  }
  
  @POST
  @Path("/reprocess")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
//# N day, n month, n year, n hour, n minute, n second
  public ServiceResponse reprocess(@QueryParam("eventName") String eventName,
      @QueryParam("ruleName") String ruleName, @QueryParam("backfill") boolean backfill,
      @QueryParam("backfillTime") String backfillTime) {
    long timeBackfill = StringUtils.getBackFillTime(backfillTime);
    if(timeBackfill==-1){
      return new ServiceResponse("input backfillTime is wrong", false);
    }
    try {
      List<EventQuery> rules =
          CassandraRepository.getInstance(Configurations.cassandraHost, Configurations.cassandraKeyspace).getEventQueryFromDB(eventName, ruleName);
      if (rules != null && !rules.isEmpty()) {
        EventQuery rule = rules.get(0);
        EventHandlerLaunch.readerEsperProcessor.stop();
        
        boolean result = EventHandlerLaunch.esperProcessor.reprocess(rule, backfill, timeBackfill);
        EventHandlerLaunch.readerEsperProcessor.start();
        if (result) {
          return new ServiceResponse("Change successfully", true);
        } else {
          return new ServiceResponse("Change unsuccessfully", false);
        }
      } else {
        return new ServiceResponse("No rule to process", false);
      }
    } catch (Exception e) {
      return new ServiceResponse("Change unsuccessfully", false);
    }
  }
  
  @POST
  @Path("/reprocessAll")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
//# N day, n month, n year, n hour, n minute, n second
  public ServiceResponse reprocessAll(@QueryParam("backfill") boolean backfill,
      @QueryParam("backfillTime") String backfillTime) {
    long timeBackfill = StringUtils.getBackFillTime(backfillTime);
    if(timeBackfill==-1){
      return new ServiceResponse("input backfillTime is wrong", false);
    }
    try {
      List<EventQuery> rules =
          CassandraRepository.getInstance(Configurations.cassandraHost, Configurations.cassandraKeyspace).getEventQueryFromDB(Configurations.kafkaEventReaderList);
      StringBuilder res = new StringBuilder();
      if (rules != null && !rules.isEmpty()) {
        EventHandlerLaunch.readerEsperProcessor.stop();
        for (EventQuery rule : rules) {
          if(Configurations.ruleList.contains(rule.getRuleName())){
            boolean result = EventHandlerLaunch.esperProcessor.reprocess(rule, backfill, timeBackfill);
            if (!result) {
              res.append(rule.getEventName() + ":" + rule.getRuleName() + ", ");
            }
          }
        }
        EventHandlerLaunch.readerEsperProcessor.start();
        if(Strings.isNullOrEmpty(res.toString())){
          return new ServiceResponse("Change successfully", true);
        }else{
          return new ServiceResponse("Change unsuccessfully with eventname:rule :" + res.toString(), false);
        }
      } else {
        return new ServiceResponse("No rule to process", false);
      }
    } catch (Exception e) {
      return new ServiceResponse("Change unsuccessfully", false);
    }
  }
}

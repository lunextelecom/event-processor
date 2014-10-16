package com.lunex.eventprocessor.handler;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventQuery.EventQueryStatus;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;

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
          CassandraRepository.getInstance().getEventQueryFromDB(eventName, ruleName);
      if (rules != null && !rules.isEmpty()) {
        EventQuery rule = rules.get(0);
        App.readerEsperProcessor.stop();
        boolean result = App.esperProcessor.updateRule(rule, backfill, backfillTime);
        App.readerEsperProcessor.start();
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
          CassandraRepository.getInstance().getEventQueryFromDB(eventName, ruleName);
      if (rules != null && !rules.isEmpty()) {
        EventQuery rule = rules.get(0);
        if (rule.getStatus() != EventQueryStatus.STOP) {
          return new ServiceResponse("Rule is stoped", false);
        }
        App.readerEsperProcessor.stop();
        boolean result = App.esperProcessor.stopRule(rule);
        App.readerEsperProcessor.start();
        if (result) {
          rule.setStatus(EventQueryStatus.STOP);
          CassandraRepository.getInstance().changeEventQueryStatus(rule);
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
          CassandraRepository.getInstance().getEventQueryFromDB(eventName, ruleName);
      if (rules != null && !rules.isEmpty()) {
        EventQuery rule = rules.get(0);
        if (rule.getStatus() != EventQueryStatus.STOP) {
          return new ServiceResponse("Rule is running", false);
        }
        App.readerEsperProcessor.stop();
        boolean result = App.esperProcessor.startRule(rule, backfill, backfillTime);
        App.readerEsperProcessor.start();
        if (result) {
          rule.setStatus(EventQueryStatus.RUNNING);
          CassandraRepository.getInstance().changeEventQueryStatus(rule);
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
}

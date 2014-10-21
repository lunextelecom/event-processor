package com.lunex.eventprocessor.webservice.rest;

import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.webservice.service.EventProcessorServiceAdmin;

@Path("/admin")
public class EventProcessorWebServiceAdminResource {

  final static org.slf4j.Logger logger = LoggerFactory
      .getLogger(EventProcessorWebServiceAdminResource.class);

  private EventProcessorServiceAdmin service;

  public EventProcessorWebServiceAdminResource(EventProcessorServiceAdmin service) {
    this.service = service;
  }

  /**
   * Add rule exception
   * 
   * @param eventName
   * @param ruleName
   * @param action
   * @param datetinme
   * @param filter
   * @return
   */
  @POST
  @Path("/add/ruleexception")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response addRuleException(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName, @QueryParam("action") String action,
      @QueryParam("expiredDate") String datetinme, @QueryParam("filter") String filter) {

    try {
      service.addRuleException(eventName, ruleName, action, datetinme, filter);
      return Response.status(Response.Status.OK).entity(new ServiceResponse("", true)).build();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(e.getMessage(), false)).build();
    }
  }

  /**
   * Add new rule
   * 
   * @param eventName
   * @param ruleName
   * @param data
   * @param fields
   * @param filters
   * @param aggregateField
   * @param having
   * @param smallBucket
   * @param bigBucket
   * @param conditions
   * @param description
   * @param autoStart : true to start rule
   * @param backfill : true to backfill
   * @param backfillTime : n day, n hour, n minute, n second
   * @return
   */
  @POST
  @Path("/add/rule")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response addRule(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName, @QueryParam("data") String data,
      @QueryParam("fields") String fields, @QueryParam("filters") String filters,
      @QueryParam("aggregateField") String aggregateField, @QueryParam("having") String having,
      @QueryParam("smallBucket") String smallBucket, @QueryParam("bigBucket") String bigBucket,
      @QueryParam("conditions") String conditions, @QueryParam("description") String description,
      @QueryParam("autoStart") Boolean autoStart, @QueryParam("backfill") Boolean backfill,
      @QueryParam("backfillTime") String backfillTime) {
    if (Strings.isNullOrEmpty(eventName) || Strings.isNullOrEmpty(ruleName)
        || Strings.isNullOrEmpty(data) || Strings.isNullOrEmpty(smallBucket)) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ServiceResponse("", false))
          .build();
    }
    try {
      // Add rule to db
      service.addRule(eventName, ruleName, data, fields, filters, aggregateField, having,
          smallBucket, bigBucket, conditions, description);
      // Start rule
      if (autoStart != null && autoStart) {
        Map<String, Object> map = service.startRule(eventName, ruleName, backfill, backfillTime);
        String response = JsonHelper.toJSonString(map);
        return Response.status(Response.Status.OK).entity(new ServiceResponse(response, true))
            .build();
      } else {
        return Response.status(Response.Status.OK)
            .entity(new ServiceResponse("Add rule success", true)).build();
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(e.getMessage(), false)).build();
    }
  }

  /**
   * delete rule
   * 
   * @param eventName
   * @param ruleName
   * @return
   */
  @DELETE
  @Path("/delete/rule")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response deleteRule(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName) {
    try {
      // stop rule for event processor
      Map<String, Object> map = service.stopRule(eventName, ruleName);
      Iterator<String> keys = map.keySet().iterator();
      // check result of stopping rule
      while (keys.hasNext()) {
        String key = keys.next();
        String stopResultStr = (String) map.get(key);
        // check result after stop
        if (!StringUtils.isJSONValid(stopResultStr)) {
          JSONObject json = new JSONObject(stopResultStr);
          if (!json.getBoolean("result")) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ServiceResponse("Can not stop rule at " + key, false)).build();
          }
        } else {
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ServiceResponse("Can not stop rule at " + key, false)).build();
        }
      }
      // delete rule after stop success all event handler
      service.deleteRule(eventName, ruleName);
      return Response.status(Response.Status.OK).entity(new ServiceResponse("", true)).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(ex.getMessage(), false)).build();
    }
  }

  /**
   * Stop rule
   * 
   * @param eventName
   * @param ruleName
   * @return
   */
  @POST
  @Path("/stop/rule")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response stopRule(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName) {
    try {
      Map<String, Object> map = service.stopRule(eventName, ruleName);
      String stopResponse = JsonHelper.toJSonString(map);
      return Response.status(Response.Status.OK).entity(new ServiceResponse(stopResponse, true))
          .build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(ex.getMessage(), false)).build();
    }
  }

  @POST
  @Path("/change/rule")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response updateRule(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName, @QueryParam("data") String data,
      @QueryParam("fields") String fields, @QueryParam("filters") String filters,
      @QueryParam("aggregateField") String aggregateField, @QueryParam("having") String having,
      @QueryParam("smallBucket") String smallBucket, @QueryParam("bigBucket") String bigBucket,
      @QueryParam("conditions") String conditions, @QueryParam("description") String description,
      @QueryParam("autoStart") Boolean autoStart, @QueryParam("backfill") Boolean backfill,
      @QueryParam("backfillTime") String backfillTime) {
    try {
      service.updateRule(eventName, ruleName, data, fields, filters, aggregateField, having,
          smallBucket, bigBucket, conditions, description);
      if (autoStart != null && autoStart) {
        Map<String, Object> map = service.changeRule(eventName, ruleName, backfill, backfillTime);
        String response = JsonHelper.toJSonString(map);
        return Response.status(Response.Status.OK).entity(new ServiceResponse(response, true))
            .build();
      } else {
        return Response.status(Response.Status.OK)
            .entity(new ServiceResponse("Update rule is success", true)).build();
      }
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(e.getMessage(), false)).build();
    }
  }
}

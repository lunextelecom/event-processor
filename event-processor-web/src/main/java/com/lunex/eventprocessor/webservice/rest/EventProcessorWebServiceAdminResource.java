package com.lunex.eventprocessor.webservice.rest;

import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
import com.wordnik.swagger.annotations.*;

@Path("/admin")
@Api(value = "/admin", description = "Operations about admin")
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
  @Path("/rule-exception")
  @ApiOperation(value = "Add rule exception for rule", notes = "Add rule exception for rule",
      response = Response.class)
  @ApiResponses(value = {@ApiResponse(code = 500, message = "INTERNAL_SERVER_ERROR"),
      @ApiResponse(code = 200, message = "OK")})
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
  @Path("/rule")
  @ApiOperation(
      value = "Add new rule",
      notes = "bodyData={\"evtName\": \"new_order\", \"ruleName\": \"rule1\", \"data\":\"new_order\", \"fields\" : \"sum(amount:double), acctNum:string\", \"filters\":\"acctNum:string='PC01D001'\", \"aggregateField\": \"acctNum:string\", \"having\":\"sum(amount:double) > 10.0\", \"smallBucket\": \"10 second\", \"bigBucket\": \"1 hour\", \"conditions\":\"sum(amount) > 50 && sum(amount) < 70\", \"type\": \"0\", \"weight\":\"0\", \"description\": \"description\", \"autoStart\": true, \"backfill\": true, \"backfillTime\": \"1 day\"}",
      response = Response.class)
  @ApiResponses(value = {@ApiResponse(code = 500, message = "INTERNAL_SERVER_ERROR"),
      @ApiResponse(code = 200, message = "OK")})
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  /**
   * 
   * @param eventName: name of event
   * @param ruleName : name of rule
   * @param bodyData: data of rule with json format
   * {
   * "evtName": "new_order", "ruleName": "rule1", "data":"new_order", "fields" : "sum(amount:double), acctNum:string", "filters":"acctNum:string='PC01D001'", "aggregateField": "acctNum:string",
   * "having":"sum(amount:double) > 10.0", "smallBucket": "10 second", "bigBucket": "1 hour", "conditions":"sum(amount) > 50 && sum(amount) < 70", "type": "0", "weight":"0", "description": "description", "autoStart": true, "backfill": true, "backfillTime": "1 day"
   * }
   * @return
   */
  public Response addRule(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName, String bodyData) {
    try {
      JSONObject json = new JSONObject(bodyData);
      String data = null;
      if (json.has("data")) {
        data = json.getString("data");
      }
      String fields = null;
      if (json.has("fields")) {
        fields = json.getString("fields");
      }
      String filters = null;
      if (json.has("filters")) {
        filters = json.getString("filters");
      }
      String aggregateField = null;
      if (json.has("aggregateField")) {
        aggregateField = json.getString("aggregateField");
      }
      String having = null;
      if (json.has("having")) {
        having = json.getString("having");
      }
      String smallBucket = null;
      if (json.has("smallBucket")) {
        smallBucket = json.getString("smallBucket");
      }
      String bigBucket = null;
      if (json.has("bigBucket")) {
        bigBucket = json.getString("bigBucket");
      }
      String conditions = null;
      if (json.has("conditions")) {
        conditions = json.getString("conditions");
      }
      String description = null;
      if (json.has("description")) {
        description = json.getString("description");
      }
      Boolean autoStart = null;
      if (json.has("autoStart")) {
        autoStart = json.getBoolean("autoStart");
      }
      Boolean backfill = null;
      if (json.has("backfill")) {
        backfill = json.getBoolean("backfill");
      }
      String backfillTime = null;
      if (json.has("backfillTime")) {
        backfillTime = json.getString("backfillTime");
      }
      Integer type = 0;
      if (json.has("type")) {
        type = json.getInt("type");
      }
      Integer weight = 0;
      if (json.has("weight")) {
        weight = json.getInt("weight");
      }

      if (Strings.isNullOrEmpty(eventName) || Strings.isNullOrEmpty(ruleName)
          || Strings.isNullOrEmpty(data) || Strings.isNullOrEmpty(smallBucket)) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new ServiceResponse("", false))
            .build();
      }

      // Add rule to db
      service.addRule(eventName, ruleName, data, fields, filters, aggregateField, having, type,
          weight, smallBucket, bigBucket, conditions, description, null);
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
  @Path("/rule")
  @ApiOperation(value = "Delete rule", notes = "Delete rule", response = Response.class)
  @ApiResponses(value = {@ApiResponse(code = 500, message = "INTERNAL_SERVER_ERROR"),
      @ApiResponse(code = 200, message = "OK")})
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
        if (StringUtils.isJSONValid(stopResultStr)) {
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
  @PUT
  @Path("/rule/stop")
  @ApiOperation(value = "Stop rule", notes = "Stop rule", response = Response.class)
  @ApiResponses(value = {@ApiResponse(code = 500, message = "INTERNAL_SERVER_ERROR"),
      @ApiResponse(code = 200, message = "OK")})
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

  /**
   * Start a rule
   * 
   * @param eventName
   * @param ruleName
   * @return
   */
  @PUT
  @Path("/rule/start")
  @ApiOperation(value = "Start rule", notes = "Start rule", response = Response.class)
  @ApiResponses(value = {@ApiResponse(code = 500, message = "INTERNAL_SERVER_ERROR"),
      @ApiResponse(code = 200, message = "OK")})
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response startRule(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName, @QueryParam("backfill") Boolean backfill,
      @QueryParam("backfillTime") String backfillTime) {
    try {
      Map<String, Object> map = service.startRule(eventName, ruleName, backfill, backfillTime);
      String startResponse = JsonHelper.toJSonString(map);
      return Response.status(Response.Status.OK).entity(new ServiceResponse(startResponse, true))
          .build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(ex.getMessage(), false)).build();
    }
  }

  @PUT
  @Path("/rule")
  @ApiOperation(
      value = "Update rule",
      notes = "bodyData={\"evtName\": \"new_order\", \"ruleName\": \"rule1\", \"data\":\"new_order\", \"fields\" : \"sum(amount:double), acctNum:string\", \"filters\":\"acctNum:string='PC01D001'\", \"aggregateField\": \"acctNum:string\", \"having\":\"sum(amount:double) > 10.0\", \"smallBucket\": \"10 second\", \"bigBucket\": \"1 hour\", \"conditions\":\"sum(amount) > 50 && sum(amount) < 70\", \"type\": \"0\", \"weight\":\"0\", \"description\": \"description\", \"autoStart\": true, \"backfill\": true, \"backfillTime\": \"1 day\"}",
      response = Response.class)
  @ApiResponses(value = {@ApiResponse(code = 500, message = "INTERNAL_SERVER_ERROR"),
      @ApiResponse(code = 200, message = "OK")})
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  /**
   * Update rule
   * @param eventName : name of event
   * @param ruleName : name of rule
   * @param bodyData : data of rule with json format
   * {
   * "evtName": "new_order", "ruleName": "rule1", "data":"new_order", "fields" : "sum(amount:double), acctNum:string", "filters":"acctNum:string='PC01D001'", "aggregateField": "acctNum:string".
   * "having":"sum(amount:double) > 10.0", "smallBucket": "10 second", "bigBucket": "1 hour", "conditions":"sum(amount) > 50 && sum(amount) < 70", "type": "0", "weight":"0", "description": "description", "autoStart": true, "backfill": true, "backfillTime": "1 day"
   * } 
   * @return
   */
  public Response updateRule(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName, String bodyData) {
    try {
      JSONObject json = new JSONObject(bodyData);
      String data = null;
      if (json.has("data")) {
        data = json.getString("data");
      } else {
        data = eventName.replace(" s+", "_");
      }
      String fields = null;
      if (json.has("fields")) {
        fields = json.getString("fields");
      }
      String filters = null;
      if (json.has("filters")) {
        filters = json.getString("filters");
      }
      String aggregateField = null;
      if (json.has("aggregateField")) {
        aggregateField = json.getString("aggregateField");
      }
      String having = null;
      if (json.has("having")) {
        having = json.getString("having");
      }
      String smallBucket = null;
      if (json.has("smallBucket")) {
        smallBucket = json.getString("smallBucket");
      }
      String bigBucket = null;
      if (json.has("bigBucket")) {
        bigBucket = json.getString("bigBucket");
      }
      String conditions = null;
      if (json.has("conditions")) {
        conditions = json.getString("conditions");
      }
      String description = null;
      if (json.has("description")) {
        description = json.getString("description");
      }
      Boolean autoStart = null;
      if (json.has("autoStart")) {
        autoStart = json.getBoolean("autoStart");
      }
      Boolean backfill = null;
      if (json.has("backfill")) {
        backfill = json.getBoolean("backfill");
      }
      String backfillTime = null;
      if (json.has("backfillTime")) {
        backfillTime = json.getString("backfillTime");
      }
      Integer type = 0;
      if (json.has("type")) {
        type = json.getInt("type");
      }
      Integer weight = 0;
      if (json.has("weight")) {
        weight = json.getInt("weight");
      }

      service.updateRule(eventName, ruleName, data, fields, filters, aggregateField, having, type,
          weight, smallBucket, bigBucket, conditions, description, autoStart);
      if (autoStart != null && autoStart) {
        if (backfill == null) {
          backfill = false;
        }
        Map<String, Object> map =
            service.changeRule(eventName, ruleName, backfill, backfillTime, autoStart);
        String response = JsonHelper.toJSonString(map);
        return Response.status(Response.Status.OK).entity(new ServiceResponse(response, true))
            .build();
      } else {
        return Response.status(Response.Status.OK)
            .entity(new ServiceResponse("Update rule is success", true)).build();
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(e.getMessage(), false)).build();
    }
  }
}

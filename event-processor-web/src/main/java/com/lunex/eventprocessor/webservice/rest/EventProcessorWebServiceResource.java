package com.lunex.eventprocessor.webservice.rest;

import java.util.HashMap;
// import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.lunex.eventprocessor.core.Event;
// import com.lunex.eventprocessor.core.EventProperty;
// import com.lunex.eventprocessor.core.EventQuery;
// import com.lunex.eventprocessor.core.QueryHierarchy;
// import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
// import com.lunex.eventprocessor.core.dataaccess.KairosDBClient;
// import com.lunex.eventprocessor.core.listener.ResultListener;
// import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.core.utils.StringUtils;
// import com.lunex.eventprocessor.handler.listener.CassandraWriter;
// import com.lunex.eventprocessor.handler.processor.EsperProcessor;
// import com.lunex.eventprocessor.handler.processor.Processor;
// import com.lunex.eventprocessor.handler.utils.Configurations;
import com.lunex.eventprocessor.webservice.service.EventProcessorService;
import com.wordnik.swagger.annotations.*;

/**
 * Dropwizard resource for event processor web api
 * 
 * @author My PC
 *
 */
@Path("/event")
@Api(value = "/event", description = "Operations about event")
public class EventProcessorWebServiceResource {

  final static org.slf4j.Logger logger = LoggerFactory
      .getLogger(EventProcessorWebServiceResource.class);

  private static long seq = 0;
  private EventProcessorService service;


  // public static List<EventProperty> listEventProperty;
  // public static QueryHierarchy hierarchy;
  // public static KairosDBClient kairosDB;
  // public static Processor esperProcessor;

  /**
   * Constructor
   * 
   * @param service
   */
  public EventProcessorWebServiceResource(EventProcessorService service) {
    // initLoadTest();
    this.service = service;
  }

  /**
   * Add new event
   * 
   * @param eventName
   * @param result
   * @param bodyData
   * @return
   */
  @POST
  @Path("/")
  @ApiOperation(value = "Add new event", notes = "Add new evemt", response = Response.class)
  @ApiResponses(value = {@ApiResponse(code = 500, message = "INTERNAL_SERVER_ERROR"),
      @ApiResponse(code = 200, message = "OK")})
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response addEvent(@QueryParam("evtName") String eventName,
      @QueryParam("result") Boolean result, String bodyData) {
    // Create event
    Event event = new Event();
    if (!Strings.isNullOrEmpty(eventName)) {
      event.setEvtName(eventName);
      try {
        event.setPayLoadStr(bodyData);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ServiceResponse(e.getMessage(), false)).build();
      }
    } else {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ServiceResponse("", false))
          .build();
    }

    seq += 1;
    try {
      // call service to get hashKey
      String hashKey = service.addEvent(event, seq);
      if (Strings.isNullOrEmpty(hashKey)) {
        return Response.status(Response.Status.OK).entity(new ServiceResponse("", false)).build();
      }

      // If client do not want to check result
      if (result == null || !result) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("hashKey", hashKey);
        return Response.status(Response.Status.OK)
            .entity(new ServiceResponse(JsonHelper.toJSonString(message), true)).build();

        // Else If client want to check result
      } else {
        // TODO maybe change to get result from kafka?
        int numRetry = 10;
        String resultCheck = null;
        while (resultCheck == null && numRetry > 0) {// check result and
          // retry
          resultCheck = service.checkEvent(eventName, hashKey);
          Thread.sleep(1000);
          numRetry--;
        }
        logger.info(resultCheck);
        return Response.status(Response.Status.OK).entity(new ServiceResponse(resultCheck, true))
            .build();
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(e.getMessage(), false)).build();
    }
  }

  /**
   * Check result from hashKey
   * 
   * @param evtName
   * @param hashKey
   * @return
   */
  @GET
  @Path("/")
  @ApiOperation(value = "Check event by hashKey", notes = "Check event", response = Response.class)
  @ApiResponses(value = {@ApiResponse(code = 500, message = "INTERNAL_SERVER_ERROR"),
      @ApiResponse(code = 200, message = "OK")})
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response checkWithHashkey(@QueryParam("evtName") String eventName,
      @QueryParam("hashKey") String hashKey) {
    if (Strings.isNullOrEmpty(hashKey)) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ServiceResponse("", false))
          .build();
    }
    try {
      // Check result by hashKey
      String eventResult = this.service.checkEvent(eventName, hashKey);
      // JSONObject eventResultJSon = new JSONObject(eventResult);
      // JSONArray resultArray = eventResultJSon.getJSONArray("result");
      // String temp = (String) resultArray.get(0);
      // resultArray = new JSONArray(temp);
      // for (int i = 0; i < resultArray.length(); i++) {
      // JSONObject rule = resultArray.getJSONObject(i);
      // System.out.println(rule);
      // JSONObject result_event = rule.getJSONObject("result-event");
      // System.out.println(result_event);
      // }
      return Response.status(Response.Status.OK).entity(new ServiceResponse(eventResult, true))
          .build();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(null, false)).build();
    }
  }

  /**
   * Check result from event
   * 
   * @param httpRequest
   * @return
   */
  @POST
  @Path("/check")
  @ApiOperation(value = "Check event by event data", notes = "Check event",
      response = Response.class)
  @ApiResponses(value = {@ApiResponse(code = 500, message = "INTERNAL_SERVER_ERROR"),
      @ApiResponse(code = 200, message = "OK")})
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response check(@QueryParam("evtName") String eventName, String bodyData) {
    try {
      if (Strings.isNullOrEmpty(eventName)) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new ServiceResponse("", false))
            .build();
      }
      // Create hashkey
      String hashKey = StringUtils.md5Java(bodyData);
      // Check result
      String eventResult = service.checkEvent(eventName, hashKey);
      return Response.status(Response.Status.OK).entity(new ServiceResponse(eventResult, true))
          .build();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(null, false)).build();
    }
  }

  // @POST
  // @Path("/loadtest")
  // @Produces(MediaType.APPLICATION_JSON)
  // @Timed
  // public Response loadTest(@QueryParam("evtName") String eventName,
  // @QueryParam("result") Boolean result, String bodyData) {
  // try {
  // Event event = new Event(bodyData);
  // event.setEvtName(eventName);
  // esperProcessor.consume(event);
  // String hashKey = StringUtils.md5Java(bodyData);
  // int numRetry = 10;
  // String resultCheck = null;
  // while (resultCheck == null && numRetry > 0) {// check result and
  // // retry
  // resultCheck = service.checkEvent(eventName, hashKey);
  // Thread.sleep(100);
  // numRetry--;
  // }
  // return Response.status(Response.Status.OK).entity(new ServiceResponse(resultCheck,
  // true)).build();
  // } catch (Exception ex) {
  // return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
  // .entity(new ServiceResponse(null, false)).build();
  // }
  // }

  // public void initLoadTest() {
  // try {
  //
  // // get EventQuery
  // hierarchy = new QueryHierarchy();
  // List<EventQuery> listEventQuery =
  // CassandraRepository.getInstance("10.9.9.133", "event_processor").getEventQueryFromDB("",
  // "");
  // if (listEventQuery != null && !listEventQuery.isEmpty()) {
  // List<List<EventQuery>> grouping =
  // EventQueryProcessor.groupEventQueryByEventName(listEventQuery);
  // // get Eventproperties
  // listEventProperty = EventQueryProcessor.processEventProperyForEventQuery(listEventQuery);
  //
  // // Create QueryHierarchy
  // for (int i = 0; i < grouping.size(); i++) {
  // List<EventQuery> subList = grouping.get(i);
  // for (int j = 0; j < subList.size(); j++) {
  // EventQuery query = subList.get(j);
  // if (Configurations.ruleList != null && !Configurations.ruleList.isEmpty()
  // && !Configurations.ruleList.contains(query.getRuleName())) {
  // continue;
  // }
  // hierarchy.addQuery(query.getEventName(), query,
  // new ResultListener[] {new CassandraWriter()});
  // }
  // }
  // } else {
  // logger.info("No rule in DB, please check again!");
  // }
  //
  // // create esper processor
  // esperProcessor = new EsperProcessor(hierarchy, listEventProperty, listEventQuery, false, 0L);
  // } catch (Exception ex) {
  // logger.error(ex.getMessage(), ex);
  // }
  // }
}

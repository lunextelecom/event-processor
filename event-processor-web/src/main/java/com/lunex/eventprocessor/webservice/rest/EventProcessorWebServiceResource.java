package com.lunex.eventprocessor.webservice.rest;

import java.util.HashMap;
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
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.webservice.service.EventProcessorService;

@Path("/")
public class EventProcessorWebServiceResource {

  final static org.slf4j.Logger logger = LoggerFactory
      .getLogger(EventProcessorWebServiceResource.class);

  private static long seq = 0;
  private EventProcessorService service;

  public EventProcessorWebServiceResource(EventProcessorService service) {
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
  @Path("/event")
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
        int numRetry = 10;
        String resultCheck = null;
        while (resultCheck == null && numRetry > 0) {// check result and
          // retry
          resultCheck = service.checkEvent(hashKey);
          Thread.sleep(50);
          numRetry--;
        }
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
   * @param hashKey
   * @return
   */
  @GET
  @Path("/event")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response check(@QueryParam("hashKey") String hashKey) {
    if (Strings.isNullOrEmpty(hashKey)) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ServiceResponse("", false))
          .build();
    }
    try {
      String eventResult = service.checkEvent(hashKey);
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
  @Path("/event/check")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response check(@QueryParam("evtName") String eventName, String bodyData) {
    try {
      if (Strings.isNullOrEmpty(eventName)) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new ServiceResponse("", false))
            .build();
      }
      String eventResult = service.checkEvent(StringUtils.md5Java(bodyData));
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(eventResult, true)).build();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(null, false)).build();
    }
  }
}

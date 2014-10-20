package com.lunex.eventprocessor.webservice.rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.utils.JsonHelper;
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

  @POST
  @Path("/event")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response addEvent(@Context HttpServletRequest httpRequest) {
    boolean checkResult = false;
    Event event = new Event();
    // TODO create event
    if (httpRequest != null) {
      String checkResultString = httpRequest.getParameter("result");
      if (!Strings.isNullOrEmpty(checkResultString)) {
        checkResult = Boolean.valueOf(checkResultString);
      }
      Map<String, String[]> map = httpRequest.getParameterMap();
      event.setEvtName(map.get("evtName")[0]);
      JSONObject requestJSonObj = null;
      requestJSonObj = new JSONObject();
      Iterator<?> keys = map.keySet().iterator();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        requestJSonObj.put(key, map.get(key)[0]);
      }
      try {
        event.setPayLoadStr(requestJSonObj.toString());
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

      // if client do not want to check result
      if (!checkResult) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("hashKey", hashKey);
        return Response.status(Response.Status.OK)
            .entity(new ServiceResponse(JsonHelper.toJSonString(message), true)).build();

        // if client want to check result
      } else {
        return Response.status(Response.Status.OK)
            .entity(new ServiceResponse(service.checkEvent(hashKey), true)).build();
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(e.getMessage(), false)).build();
    }
  }

  @GET
  @Path("/check_event")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response check(@QueryParam("hashKey") String hashKey) {
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

  @GET
  @Path("/event")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response check(@Context HttpServletRequest httpRequest) {
    try {
      String eventResult = service.checkEvent("abc");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(eventResult, true)).build();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(null, false)).build();
    }
  }
}

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

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
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
  public ServiceResponse addEvent(@Context HttpServletRequest httpRequest) {
    Event event = new Event();
    // TODO create event
    if (httpRequest != null) {
      httpRequest.getAttribute("amount");
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
      }
    }

    seq += 1;
    try {
      // call service to get hashKey
      String hashKey = service.addEvent(event, seq);
      if (hashKey == null) {
        return new ServiceResponse(null, false);
      }
      Map<String, Object> message = new HashMap<String, Object>();
      message.put("hashKey", hashKey);
      return new ServiceResponse(JsonHelper.toJSonString(message), true);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return new ServiceResponse(e.getMessage(), false);
    }
  }

  @GET
  @Path("/event")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ServiceResponse check(@QueryParam("hashKey") String hashKey) {
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
      return new ServiceResponse(eventResult, true);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return new ServiceResponse(null, false);
  }
}
package com.lunex.eventprocessor.webservice.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.webservice.service.EventProcessorServiceAdmin;

@Path("/event-processor")
public class EventProcessorWebServiceAdminResource {

  final static org.slf4j.Logger logger = LoggerFactory
      .getLogger(EventProcessorWebServiceAdminResource.class);

  private EventProcessorServiceAdmin service;

  public EventProcessorWebServiceAdminResource(EventProcessorServiceAdmin service) {
    this.service = service;
  }

  @POST
  @Path("/add-event")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ServiceResponse addRule() {
    Map<String, Object> message = new HashMap<String, Object>();

    try {
      return new ServiceResponse(JsonHelper.toJSonString(message), true);
    } catch (Exception e) {
      return new ServiceResponse(e.getMessage(), false);
    }
  }
}

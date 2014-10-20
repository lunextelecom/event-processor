package com.lunex.eventprocessor.webservice.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.webservice.service.EventProcessorService;

@Path("/event-processor")
public class EventProcessorWebServiceResource {

  final static org.slf4j.Logger logger = LoggerFactory
      .getLogger(EventProcessorWebServiceResource.class);

  private EventProcessorService service;

  public EventProcessorWebServiceResource(EventProcessorService service) {
    this.service = service;
  }

  @GET
  @Path("/test")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ServiceResponse resetEPL() {
    return new ServiceResponse("test", true);
  }
  
  @POST
  @Path("/add-event")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ServiceResponse addEvent() {
    Map<String, Object> message = new HashMap<String, Object>();
    message.put("hashKey", "");
    
    
    
    try {
      return new ServiceResponse(JsonHelper.toJSonString(message), true);
    } catch (Exception e) {
      return new ServiceResponse(e.getMessage(), false);
    } 
  }
}

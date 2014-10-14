package com.lunex.eventprocessor.handler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;

@Path("/event-processor-handler")
public class EventHandlerApiServiceResource {
  final static org.slf4j.Logger logger = LoggerFactory.getLogger(EventHandlerApiServiceResource.class);
  
  public EventHandlerApiServiceResource() {
    
  }
  
  @GET
  @Path("/resetEPL")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ServiceResponse resetEPL() {
    return new ServiceResponse("test", true);
  }
  
}

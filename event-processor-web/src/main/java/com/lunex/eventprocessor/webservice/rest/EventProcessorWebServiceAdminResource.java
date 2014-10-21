package com.lunex.eventprocessor.webservice.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.lunex.eventprocessor.webservice.service.EventProcessorServiceAdmin;

@Path("/admin")
public class EventProcessorWebServiceAdminResource {

  final static org.slf4j.Logger logger = LoggerFactory
      .getLogger(EventProcessorWebServiceAdminResource.class);

  private EventProcessorServiceAdmin service;

  public EventProcessorWebServiceAdminResource(EventProcessorServiceAdmin service) {
    this.service = service;
  }

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

  @POST
  @Path("/add/rule")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response addRule(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName, @QueryParam("data") String data,
      @QueryParam("fields") String fields, @QueryParam("filters") String filters,
      @QueryParam("aggregateField") String aggregateField, @QueryParam("having") String having,
      @QueryParam("smallBucket") String smallBucket, @QueryParam("bigBucket") String bigBucket,
      @QueryParam("conditions") String conditions, @QueryParam("description") String description) {
    if (Strings.isNullOrEmpty(eventName) || Strings.isNullOrEmpty(ruleName)
        || Strings.isNullOrEmpty(data) || Strings.isNullOrEmpty(smallBucket)) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ServiceResponse("", false))
          .build();
    }
    try {
      service.addRule(eventName, ruleName, data, fields, filters, aggregateField, having,
          smallBucket, bigBucket, conditions, description);
      return Response.status(Response.Status.OK).entity(new ServiceResponse("", true)).build();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(e.getMessage(), false)).build();
    }
  }

  @DELETE
  @Path("/delete/rule")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response deleteRule(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName) {
    try {
      service.deleteRule(eventName, ruleName);
      return Response.status(Response.Status.OK).entity(new ServiceResponse("", true)).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(ex.getMessage(), false)).build();
    }
  }
}

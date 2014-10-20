package com.lunex.eventprocessor.webservice.rest;

import java.util.HashMap;
import java.util.Map;

import javax.validation.executable.ExecutableType;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.lunex.eventprocessor.core.EventQueryException;
import com.lunex.eventprocessor.core.EventQueryException.ExptionAction;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.core.utils.TimeUtil;
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
  @Path("/addruleexception")
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public Response addRuleException(@QueryParam("evtName") String eventName,
      @QueryParam("ruleName") String ruleName, @QueryParam("action") String action,
      @QueryParam("expiredDate") String datetinme, @QueryParam("filter") String filter) {

    Map<String, Object> map = new HashMap<String, Object>();
    map = JsonHelper.toMap(new JSONObject(filter));
    EventQueryException eventQueyException =
        new EventQueryException(eventName, ruleName, ExptionAction.valueOf(action),
            TimeUtil.convertStringToDate(datetinme, "dd/MM/yyyy HH:mm:ss"), map);
    try {
      service.addRuleException(eventQueyException);
      return Response.status(Response.Status.OK).entity(new ServiceResponse("", true)).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ServiceResponse(e.getMessage(), false)).build();
    }
  }
}

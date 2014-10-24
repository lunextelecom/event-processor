package com.lunex.eventprocessor.webservice.rest;

import static org.junit.Assert.*;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.webservice.service.EventProcessorService;


public class EventProcessorWebServiceResourceTest {

  static EventProcessorWebServiceFactory ccServiceFactory;

  @Test
  public void testAddEvent() {
    ccServiceFactory = new EventProcessorWebServiceFactory();
    ccServiceFactory.setDbHost("192.168.93.38");
    ccServiceFactory.setDbName("event_processor");
    ccServiceFactory.setHandlerServiceUrl(new String[] {"192.168.93.61:8088"});
    ccServiceFactory.setInputProcessorUrl("http://192.168.93.61:9038");

    try {
      EventProcessorWebServiceResource resource =
          new EventProcessorWebServiceResource(new EventProcessorService(
              CassandraRepository.getInstance(ccServiceFactory.getDbHost(),
                  ccServiceFactory.getDbName()), ccServiceFactory));
      Response response =
          resource
              .addEvent(
                  "new_order",
                  true,
                  "{\"acctNum\": \"PC01D001\", \"amount\": 65.0, \"txId\": \"124\", \"evtName\": \"new_order\", \"time\": "
                      + System.currentTimeMillis() + "}");
      System.out.println(((ServiceResponse)response.getEntity()).getMessage());
      assertEquals(true, ((ServiceResponse)response.getEntity()).getResult());
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void testCheckString() {}

  @Test
  public void testCheckStringString() {}

}

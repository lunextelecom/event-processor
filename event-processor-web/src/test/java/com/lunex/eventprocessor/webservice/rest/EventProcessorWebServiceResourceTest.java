package com.lunex.eventprocessor.webservice.rest;

import static org.junit.Assert.*;

import javax.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.Test;

import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.webservice.service.EventProcessorService;

public class EventProcessorWebServiceResourceTest {

  static EventProcessorWebServiceFactory ccServiceFactory;
  static EventProcessorWebServiceResource resource;

  @BeforeClass
  public static void beforeClass() {
    ccServiceFactory = new EventProcessorWebServiceFactory();
    ccServiceFactory.setDbHost("192.168.93.38");
    ccServiceFactory.setDbName("event_processor");
    ccServiceFactory.setHandlerServiceUrl(new String[] {"192.168.93.61:8088"});
    ccServiceFactory.setInputProcessorUrl("http://192.168.93.61:9038");
    try {
      resource =
          new EventProcessorWebServiceResource(new EventProcessorService(
              CassandraRepository.getInstance(ccServiceFactory.getDbHost(),
                  ccServiceFactory.getDbName()), ccServiceFactory));
    } catch (Exception e) {
      assertEquals(true, false);
      e.printStackTrace();
    }

  }

  @Test
  public void testAddEvent() {

    try {
      Response response =
          resource
              .addEvent(
                  "new_order",
                  true,
                  "{\"acctNum\": \"PC01D001\", \"amount\": 65.0, \"txId\": \"124\", \"evtName\": \"new_order\", \"time\": "
                      + System.currentTimeMillis() + "}");
      System.out.println(((ServiceResponse) response.getEntity()).getMessage());
      assertEquals(true, ((ServiceResponse) response.getEntity()).getResult());
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void testCheckString() {
    String payload =
        "{\"acctNum\": \"PC01D001\", \"amount\": 65.0, \"txId\": \"124\", \"evtName\": \"new_order\", \"time\": "
            + System.currentTimeMillis() + "}";
    resource.addEvent("new_order", true, payload);
    try {
      Response response = null;
      int retry = 10;
      while (response == null && retry > 0) {
        response = resource.check(StringUtils.md5Java(payload));
        retry--;
      }
      assertEquals(true, ((ServiceResponse) response.getEntity()).getResult());
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void testCheckStringString() {
    String payload =
        "{\"acctNum\": \"PC01D001\", \"amount\": 65.0, \"txId\": \"124\", \"evtName\": \"new_order\", \"time\": "
            + System.currentTimeMillis() + "}";
    resource.addEvent("new_order", true, payload);
    try {
      Response response = null;
      int retry = 10;
      while (response == null && retry > 0) {
        response = resource.check("new_order", StringUtils.md5Java(payload));
        retry--;
      }
      assertEquals(true, ((ServiceResponse) response.getEntity()).getResult());
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

}

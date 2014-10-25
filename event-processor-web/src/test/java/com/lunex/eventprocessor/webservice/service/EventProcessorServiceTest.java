package com.lunex.eventprocessor.webservice.service;

import static org.junit.Assert.*;


import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.webservice.rest.EventProcessorWebServiceFactory;

public class EventProcessorServiceTest {
  static EventProcessorWebServiceFactory ccServiceFactory;
  static EventProcessorService service;

  @BeforeClass
  public static void beforeClass() {
    ccServiceFactory = new EventProcessorWebServiceFactory();
    ccServiceFactory.setDbHost("192.168.93.38");
    ccServiceFactory.setDbName("event_processor");
    ccServiceFactory.setHandlerServiceUrl(new String[] {"192.168.93.61:8088"});
    ccServiceFactory.setInputProcessorUrl("http://192.168.93.61:9038");
    try {
      service =
          new EventProcessorService(CassandraRepository.getInstance(ccServiceFactory.getDbHost(),
              ccServiceFactory.getDbName()), ccServiceFactory);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void testAddEvent() {
    String payload =
        "{\"acctNum\": \"PC01D001\", \"amount\": 65.0, \"txId\": \"124\", \"evtName\": \"new_order\", \"time\": "
            + System.currentTimeMillis() + "}";
    try {
      String hashKey = StringUtils.md5Java(payload);
      Event event = new Event(payload);
      String result = service.addEvent(event, 1);
      assertEquals(hashKey, result);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void testCheckEvent() {
    String payload =
        "{\"acctNum\": \"PC01D001\", \"amount\": 65.0, \"txId\": \"124\", \"evtName\": \"new_order\", \"time\": "
            + System.currentTimeMillis() + "}";
    try {
      Event event = new Event(payload);
      String hashKey = service.addEvent(event, 1);
      String result = null;
      int retry = 10;
      while (result == null && retry > 0) {
        result = service.checkEvent(hashKey);
        Thread.sleep(1000);
        retry--;
      }
      if (result != null) {
        JSONObject json = new JSONObject(result);
        if(json.has("result")) {
          assertEquals(true, true);
          return;
        }
      }
      assertEquals(true, false);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

}

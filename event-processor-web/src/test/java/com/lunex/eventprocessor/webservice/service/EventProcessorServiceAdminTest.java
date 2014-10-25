package com.lunex.eventprocessor.webservice.service;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventQuery.EventQueryStatus;
import com.lunex.eventprocessor.core.EventQueryException;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.webservice.rest.EventProcessorWebServiceFactory;

public class EventProcessorServiceAdminTest {
  static EventProcessorWebServiceFactory ccServiceFactory;
  static EventProcessorServiceAdmin service;
  
  @BeforeClass
  public static void beforeClass() {
    ccServiceFactory = new EventProcessorWebServiceFactory();
    ccServiceFactory.setDbHost("192.168.93.38");
    ccServiceFactory.setDbName("event_processor");
    ccServiceFactory.setHandlerServiceUrl(new String[] {"192.168.93.61:8088"});
    ccServiceFactory.setInputProcessorUrl("http://192.168.93.61:9038");
    try {
      service =
          new EventProcessorServiceAdmin(CassandraRepository.getInstance(ccServiceFactory.getDbHost(),
              ccServiceFactory.getDbName()), ccServiceFactory);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }
  
  @Test
  public void testAddRuleException() {
    try {
      service.addRuleException("test_event", "test_rule_name", EventQueryException.ExptionAction.VERIFIED.toString(), "12/12/2018 12:00:00", "");
      EventQuery eventquery =new EventQuery();
      eventquery.setEventName("test_event");
      eventquery.setRuleName("test_rule_name");
      List<EventQueryException> list = CassandraRepository.getInstance(ccServiceFactory.getDbHost(),
          ccServiceFactory.getDbName()).getEventQueyExceptionNotExpired(eventquery, EventQueryException.ExptionAction.VERIFIED.toString());
      if (list != null && !list.isEmpty()) {
        if (list.get(0).getRuleName().equals("test_rule_name")) {
          assertEquals(true, true);
          return;
        }
      }
      assertEquals(true, false);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void testAddAndDeleteRule() {
    try {
      service.addRule("test_event", "test_rule_name", "test_event", "amount:int", "amount > 0", "", "", "1 minute", "", "amount > 10", "", EventQueryStatus.STOP);
      List<EventQuery> list = CassandraRepository.getInstance(ccServiceFactory.getDbHost(),
          ccServiceFactory.getDbName()).getEventQueryFromDB("test_event", "test_rule_name");
      if (list != null && !list.isEmpty()) {
        if (list.get(0).getRuleName().equals("test_rule_name")) {
          assertEquals(true, true);
        }
      }
      
      service.updateRule("test_event", "test_rule_name", "test_event", "amount:double", "amount > 0", "", "", "1 minute", "", "amount > 10", "", false);
      list = CassandraRepository.getInstance(ccServiceFactory.getDbHost(),
          ccServiceFactory.getDbName()).getEventQueryFromDB("test_event", "test_rule_name");
      if (list != null && !list.isEmpty()) {
        if (list.get(0).getFields().equals("amount:double")) {
          assertEquals(true, true);
        }
      }
      
      service.deleteRule("test_event", "test_rule_name");
      list = CassandraRepository.getInstance(ccServiceFactory.getDbHost(),
          ccServiceFactory.getDbName()).getEventQueryFromDB("test_event", "test_rule_name");
      if (list == null) {
        assertEquals(true, true);
        return;
      }
      assertEquals(true, false);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }
}

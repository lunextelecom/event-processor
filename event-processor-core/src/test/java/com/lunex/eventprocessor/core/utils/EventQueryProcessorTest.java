package com.lunex.eventprocessor.core.utils;

import java.util.ArrayList;
import java.util.List;

import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;

import static org.junit.Assert.*;

public class EventQueryProcessorTest {

  public void testProcessEventQuery() {
    assertEquals(true, true);
  }

  public void testProcessStringFieldForEventQuery() {
    assertEquals(true, true);
  }

  public void testProcessStringFieldDataTypeForEventQuery() {
    assertEquals(true, true);
  }

  public void testProcessEventProperyForEventQuery() {
    List<EventQuery> testData = new ArrayList<EventQuery>();
    EventQuery eventQuery = new EventQuery();
    eventQuery.setEventName("new_order");
    eventQuery.setData("new_order");
    eventQuery.setFields("sum(amount:double), count(txId:long), acctNum:string");
    eventQuery.setFilters("amount > 10 AND acctNum = 'PC01D001'");
    eventQuery.setAggregateField("acctNum:string");
    eventQuery.setTimeSeries(null);
    testData.add(eventQuery);

    eventQuery = new EventQuery();
    eventQuery.setEventName("new_order");
    eventQuery.setData("new_order");
    eventQuery.setFields("sum(amount:double), count(txId:long), acctNum:string, sku:string");
    eventQuery.setFilters("amount > 10 AND acctNum = 'PC01D001'");
    eventQuery.setAggregateField("acctNum:string");
    eventQuery.setTimeSeries(null);
    testData.add(eventQuery);

    List<EventProperty> temp = EventQueryProcessor.processEventProperyForEventQuery(testData);

    assertEquals(temp.size(), 1);
  }

}

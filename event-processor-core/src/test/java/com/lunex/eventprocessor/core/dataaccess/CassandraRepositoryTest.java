package com.lunex.eventprocessor.core.dataaccess;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.EventQuery.EventQueryStatus;
import com.lunex.eventprocessor.core.utils.StringUtils;

public class CassandraRepositoryTest {

  static CassandraRepository instance;

  @BeforeClass
  public static void beforeClass() {
    System.out.println("Before Class");
    try {
      instance = CassandraRepository.getInstance("192.168.93.38", "event_processor");
      instance.execute("use event_processor", null);
      instance.execute("create table test(id bigint, content text, PRIMARY KEY (id)) ", null);
      assertEquals(1, 1);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void executeQuery() {
    System.out.println("executeQuery");

    try {
      instance.execute("insert into test(id, content) values(1, 'test')", null);
      assertEquals(true, true);

      List<Object> params = new ArrayList<Object>();
      params.add((long) 1);
      ResultSet results = instance.execute("select * from test where id = ?", params);
      for (Row row : results) {
        assertEquals("test", row.getString("content"));
      }

    } catch (Exception e) {
      System.out.println(e.getMessage());
      assertEquals(true, false);
    }
  }

  @Test
  public void insertAndGetEventToDB() {
    Event event = new Event("{'evtName': 'test_event', 'time': 1}");
    try {
      instance.insertEventToDB(event);
      List<Event> listEvents = instance.getEvent(0);
      int size = listEvents.size();
      if (listEvents != null && listEvents.size() > 0) {
        assertEquals(true, true);
      } else {
        assertEquals(true, false);
      }
      instance.execute("DELETE FROM events WHERE event_name = 'test_event'", null);
      listEvents = instance.getEvent(0);
      if (listEvents == null || listEvents.size() == size - 1) {
        assertEquals(true, true);
      } else {
        assertEquals(true, false);
      }
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void insertAndGetAndDeleteRule() {
    EventQuery eventQuery = new EventQuery();
    eventQuery.setEventName("unit_test_event");
    eventQuery.setRuleName("unit_test_event");
    eventQuery.setStatus(EventQueryStatus.STOP);
    try {
      instance.insertEventQuery(eventQuery);
      List<EventQuery> list = instance.getEventQueryFromDB("unit_test_event", "unit_test_event");
      if (list != null && list.size() == 1 && list.get(0).getEventName().equals("unit_test_event")) {
        assertEquals(true, true);
      } else {
        assertEquals(true, false);
      }
      instance.deleteEventQuery(eventQuery);
      list = instance.getEventQueryFromDB("unit_test_event", "unit_test_event");
      if (list == null) {
        assertEquals(true, true);
      } else {
        assertEquals(true, false);
      }
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }

  @Test
  public void insertAndGetResult() {
    try {
      EventResult result =
          new EventResult("test_event_result", StringUtils.md5Java("test_event_result"), null, null);
      instance.insertResults(result);
      List<EventResult> list = instance.getEventResult(StringUtils.md5Java("test_event_result"));
      if (list != null && list.size() == 1
          && list.get(0).getEventName().equals("test_event_result")) {
        assertEquals(true, true);
      } else {
        assertEquals(true, false);
      }
      instance.execute("DELETE FROM results WHERE event_name = 'test_event_result'", null);
      list = instance.getEventResult(StringUtils.md5Java("test_event_result"));
      if (list == null) {
        assertEquals(true, true);
      } else {
        assertEquals(true, false);
      }
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }
  
  @AfterClass
  public static void afterClass() {
    System.out.println("After Class");
    try {
      instance.execute("drop table test ", null);
      assertEquals(true, true);
    } catch (Exception e) {
      assertEquals(true, false);
    }
  }
}

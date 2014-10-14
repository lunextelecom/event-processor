package com.lunex.eventprocessor.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;

import net.hydromatic.linq4j.Enumerator;
import net.hydromatic.linq4j.Grouping;
import net.hydromatic.linq4j.Linq4j;
import net.hydromatic.linq4j.function.*;

public class EventQueryProcessor {

  /**
   * Create new EventQuery object which is removed datatype from properties
   * 
   * @param oldEventQuery
   * @return
   */
  public static EventQuery processEventQuery(EventQuery oldEventQuery) {
    EventQuery newEventQuery = new EventQuery();

    newEventQuery.setEventName(oldEventQuery.getEventName());
    newEventQuery.setRuleName(oldEventQuery.getRuleName());
    newEventQuery.setData(oldEventQuery.getData());
    newEventQuery.setFilters(processStringFieldForEventQuery(oldEventQuery.getFilters()));
    newEventQuery.setSmallBucket(oldEventQuery.getSmallBucket());
    newEventQuery.setBigBucket(oldEventQuery.getBigBucket());
    newEventQuery.setFields(processStringFieldForEventQuery(oldEventQuery.getFields()));
    newEventQuery.setAggregateField(processStringFieldForEventQuery(oldEventQuery
        .getAggregateField()));
    newEventQuery.setHaving(processStringFieldForEventQuery(oldEventQuery.getHaving()));
    newEventQuery.setConditions(processStringFieldForEventQuery(oldEventQuery.getConditions()));

    return newEventQuery;
  }

  /**
   * Process remove datatype from String
   * 
   * @param fieldStr
   * @return
   */
  public static String processStringFieldForEventQuery(String fieldStr) {
    String replace = fieldStr.replaceAll(":[a-zA-Z]+", "");
    return replace;
  }

  /**
   * Get datatype for properties from String
   * 
   * @param fieldStr
   * @return
   */
  public static Map<String, Object> processStringFieldDataTypeForEventQuery(String fieldStr) {
    Map<String, Object> map = new HashMap<String, Object>();
    Pattern pattern = Pattern.compile("[a-zA-Z]+:[a-zA-Z]+");
    Matcher matcher = pattern.matcher(fieldStr);
    while (matcher.find()) {
      String[] temp = matcher.group(0).split(":");
      map.put(temp[0], temp[1]);
    }
    return map;
  }

  /**
   * Group EventQuery by eventName
   * 
   * @param list
   * @return
   */
  public static List<List<EventQuery>> groupEventQueryByEventName(List<EventQuery> list) {
    if (list == null || list.size() == 0) {
      return null;
    }
    List<List<EventQuery>> results = new ArrayList<List<EventQuery>>();
    // group
    Function1<EventQuery, String> EMP_DEPTNO_SELECTOR = new Function1<EventQuery, String>() {
      public String apply(EventQuery eventQuery) {
        return eventQuery.getEventName();
      }
    };
    List<Grouping<String, EventQuery>> temp =
        Linq4j.asEnumerable(list).groupBy(EMP_DEPTNO_SELECTOR).toList();
    for (int i = 0; i < temp.size(); i++) {
      List<EventQuery> subList = new ArrayList<EventQuery>();
      Enumerator<EventQuery> enumerator = temp.get(i).enumerator();
      while (enumerator.moveNext()) {
        subList.add(enumerator.current());
      }
      results.add(subList);
    }
    return results;
  }

  /**
   * Get all properties with datatype for Event
   * 
   * @param list
   * @return
   */
  public static List<EventProperty> processEventProperyForEventQuery(List<EventQuery> list) {
    List<EventProperty> results = new ArrayList<EventProperty>();
    // group
    Function1<EventQuery, String> GroupByEventDataName = new Function1<EventQuery, String>() {
      public String apply(EventQuery eventQuery) {
        return eventQuery.getData();
      }
    };
    List<Grouping<String, EventQuery>> temp =
        Linq4j.asEnumerable(list).groupBy(GroupByEventDataName).toList();

    // create EventProperty
    EventProperty eventProperty = null;
    Map<String, Object> map = null;
    Map<String, Object> properties = null;
    for (int i = 0; i < temp.size(); i++) {
      String eventDataName = temp.get(i).getKey();
      eventProperty = new EventProperty(eventDataName, null);
      map = new HashMap<String, Object>();
      Enumerator<EventQuery> enumerator = temp.get(i).enumerator();
      properties = null;
      while (enumerator.moveNext()) {
        EventQuery eventQuery = enumerator.current();
        String fields = eventQuery.getFields();
        properties = processStringFieldDataTypeForEventQuery(fields);
        map.putAll(properties);
        String groups = eventQuery.getAggregateField();
        properties = processStringFieldDataTypeForEventQuery(groups);
        map.putAll(properties);
        String filter = eventQuery.getFilters();
        properties = processStringFieldDataTypeForEventQuery(filter);
        map.putAll(properties);
        String having = eventQuery.getHaving();
        properties = processStringFieldDataTypeForEventQuery(having);
        map.putAll(properties);
      }
      eventProperty.setProperties(map);
      results.add(eventProperty);
    }
    return results;
  }
}

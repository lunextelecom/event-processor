package com.lunex.eventprocessor.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;

import net.hydromatic.linq4j.Enumerator;
import net.hydromatic.linq4j.Grouping;
import net.hydromatic.linq4j.Linq4j;
import net.hydromatic.linq4j.function.*;

public class EventQueryProcessor {

  public static EventQuery processEventQuery(EventQuery oldEventQuery) {
    EventQuery newEventQuery = new EventQuery();

    newEventQuery.setData(oldEventQuery.getData());
    newEventQuery.setFilters(oldEventQuery.getFilters());
    newEventQuery.setTimeSeries(oldEventQuery.getTimeSeries());
    newEventQuery.setFields(processStringFieldForEventQuery(oldEventQuery.getFields()));
    newEventQuery.setAggregateField(processStringFieldForEventQuery(oldEventQuery
        .getAggregateField()));

    return newEventQuery;
  }

  public static String processStringFieldForEventQuery(String fieldStr) {
    String[] fields = fieldStr.split(",");
    StringBuilder fieldBuilder = new StringBuilder();
    for (int i = 0; i < fields.length; i++) {
      String field = fields[i].trim();
      String[] temp = field.split(":");
      if (temp.length == 1 || temp.length == 2) {
        fieldBuilder.append(temp[0]);
        if (temp[0].contains("(")) {
          fieldBuilder.append(")");
        }
        fieldBuilder.append(",");
      }
    }
    fieldBuilder.deleteCharAt(fieldBuilder.length() - 1);
    return fieldBuilder.toString();
  }

  public static Map<String, Object> processStringFieldDataTypeForEventQuery(String fieldStr) {
    String[] fields = fieldStr.split(",");
    Map<String, Object> map = new HashMap<String, Object>();
    for (int i = 0; i < fields.length; i++) {
      String field = fields[i].trim();
      if (field.contains("(")) {
        field = field.substring(field.indexOf("(") + 1, field.indexOf(")"));
      }
      String[] temp = field.split(":");
      if (temp.length == 1) {
        map.put(temp[0], "string");
      } else if (temp.length == 2) {
        map.put(temp[0], temp[1]);
      }
    }
    return map;
  }

  public static List<List<EventQuery>> groupEventQuery(List<EventQuery> list) {
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
    for (int i = 0; i < temp.size(); i++) {
      String eventDataName = temp.get(i).getKey();
      eventProperty = new EventProperty(eventDataName, null);
      map = new HashMap<String, Object>();
      Enumerator<EventQuery> enumerator = temp.get(i).enumerator();
      while (enumerator.moveNext()) {
        EventQuery eventQuery = enumerator.current();
        String fields = eventQuery.getFields();
        Map<String, Object> properties = processStringFieldDataTypeForEventQuery(fields);
        map.putAll(properties);
        String groups = eventQuery.getAggregateField();
        properties = processStringFieldDataTypeForEventQuery(groups);
        map.putAll(properties);
      }
      eventProperty.setProperties(map);
      results.add(eventProperty);
    }
    return results;
  }

}

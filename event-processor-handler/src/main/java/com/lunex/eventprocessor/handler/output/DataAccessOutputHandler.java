package com.lunex.eventprocessor.handler.output;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.event.map.MapEventBean;
import com.google.common.collect.Lists;
import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventQueryException;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.QueryHierarchy;
import com.lunex.eventprocessor.core.EventQueryException.ExptionAction;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.dataaccess.KairosDBClient;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.handler.utils.Configurations;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class DataAccessOutputHandler {

  static final Logger logger = LoggerFactory.getLogger(DataAccessOutputHandler.class);

  /**
   * Save raw event into DB
   * 
   * @param event
   */
  public static void insertRawEventToCassandra(Event event) {
    // insert raw event to db
    final Event insertEvent = event;
    Thread insertEventThread = new Thread(new Runnable() {
      public void run() {
        try {
          // insert new event
          CassandraRepository.getInstance().insertEventToDB(insertEvent);
          // insert event result --> default no violate
          CassandraRepository.getInstance().insertResults(insertEvent.getEvtName(),
              insertEvent.getHashKey(), null, null);
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
    });
    insertEventThread.start();
  }

  /**
   * Send raw event to kairosDB
   * 
   * @param event
   * @param queryHierarchy
   */
  public static void sendRawEventToKairosDB(Event event, QueryHierarchy queryHierarchy) {
    KairosDBClient client = new KairosDBClient(Configurations.kairosDBUrl);
    Map<String, String> tags = new HashMap<String, String>();
    tags.put("event-processor", event.getEvtName());
    try {
      // get hierarchy from QueryHierarchy
      Map<String, Map<EventQuery, ResultListener[]>> hierarchy = queryHierarchy.getHierarchy();
      // get map EventQuery of this event
      Map<EventQuery, ResultListener[]> eventQueries = hierarchy.get(event.getEvtName());
      // check it empty
      if (!eventQueries.isEmpty()) {
        // get list EventQuery of this event
        List<EventQuery> listEventQuery = Lists.newArrayList(eventQueries.keySet().iterator());
        // get list datatype of properties of this event
        List<EventProperty> properties =
            EventQueryProcessor.processEventProperyForEventQuery(listEventQuery);
        // create a map
        Map<String, Object> map = properties.get(0).getProperties();
        // loop to create metric from data of event
        Iterator<String> keys = map.keySet().iterator();
        String metric = event.getEvtName();
        // create metric name
        while (keys.hasNext()) {
          String key = keys.next();
          if (map.get(key).equals("string")) {
            metric += "." + key + "." + event.getEvent().get(key);
          }
        }
        // write metric to kairos DB
        keys = map.keySet().iterator();
        while (keys.hasNext()) {
          String key = keys.next();
          if (!map.get(key).equals("string")) {
            Object value = event.getEvent().get(key);
            client.sendMetric(metric + "." + key, event.getTime(), value, tags);
          }
        }
      }
    } catch (URISyntaxException e) {
      logger.error(e.getMessage(), e);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  /**
   * Write result into KairosDB
   * 
   * @param result
   * @param eventQuery
   */
  public static void writeResultToKairosDB(Object[] result, EventQuery eventQuery) {
    // create list EventQuery
    List<EventQuery> listEventQuery = new ArrayList<EventQuery>();
    listEventQuery.add(eventQuery);
    // get list datatype of properties of this event
    List<EventProperty> properties =
        EventQueryProcessor.processEventProperyForEventQuery(listEventQuery);
    // create a map
    Map<String, Object> map = properties.get(0).getProperties();
    Map<String, String> tags = new HashMap<String, String>();
    tags.put("event-processor", eventQuery.getEventName());

    KairosDBClient client = new KairosDBClient(Configurations.kairosDBUrl);
    for (int i = 0; i < result.length; i++) {
      try {
        MapEventBean item = (MapEventBean) result[i];
        Map<String, Object> resultPropeties = item.getProperties();
        // loop to create metric from data of event
        Iterator<String> keys = map.keySet().iterator();
        String metric = eventQuery.getEventName();
        // create metric name
        while (keys.hasNext()) {
          String key = keys.next();
          if (map.get(key).equals("string")) {
            for (Entry<String, Object> e : resultPropeties.entrySet()) {
              if (e.getKey().indexOf(key) == 0) {
                metric += "." + key + "." + resultPropeties.get(key);
                break;
              }
            }
          }
        }
        // write metric to kairos DB
        keys = map.keySet().iterator();
        while (keys.hasNext()) {
          String key = keys.next();
          if (map.get(key).equals("string")) {
            Object value = null;
            for (Entry<String, Object> e : resultPropeties.entrySet()) {
              if (e.getKey().indexOf(key) > 0) {
                value = e.getValue();
                client.sendMetric(metric + "." + e.getKey().replace("(", ".").replace(")", ""),
                    System.currentTimeMillis(), value, tags);
                break;
              }
            }
          } else {
            Object value = null;
            for (Entry<String, Object> e : resultPropeties.entrySet()) {
              if (e.getKey().indexOf(key) > 0) {
                value = e.getValue();
                client.sendMetric(metric + "." + e.getKey().replace("(", ".").replace(")", ""),
                    System.currentTimeMillis(), value, tags);
                break;
              }
            }
          }
        }
      } catch (Exception ex) {
        logger.error(ex.getMessage(), ex);
      }
    }
  }

  /**
   * Write result into cassandra
   * 
   * @param result
   * @param eventQuery
   */
  public static void writeResultToCassandra(Object[] result, EventQuery eventQuery) {
    if (result == null || result.length == 0) {
      return;
    }
    try {
      // get list condition excption
      List<EventQueryException> condtionExceptions =
          CassandraRepository.getInstance().getEventQueyExceptionNotExpired(eventQuery,
              ExptionAction.VERIFIED.toString());

      String eventQueryCondition = eventQuery.getConditions();
      for (int i = 0; i < result.length; i++) {
        MapEventBean item = (MapEventBean) result[i];

        // check condition exception
        boolean eventException = false;
        EventQueryException eventQueryException = null;
        if (condtionExceptions != null) {
          for (int j = 0; j < condtionExceptions.size(); j++) {
            eventQueryException = condtionExceptions.get(j);
            if (eventQueryException.getConditionFilter() == null) {
              continue;
            }
            Map<String, Object> properties = item.getProperties();
            Iterator<String> keys = properties.keySet().iterator();
            int numMappingException = 0;
            while (keys.hasNext()) {
              String key = keys.next();
              Object exception = eventQueryException.getConditionFilter().get(key);
              if (exception != null) {
                if (exception.equals(properties.get(key))) {
                  numMappingException++;
                }
              }
            }
            // this event is matched with condition exception
            if (numMappingException == eventQueryException.getConditionFilter().size()) {
              eventException = true;
              break;
            }
          }
        }

        // if exception
        if (eventException) {
          // result is false (no violate)
          logger.info("Result:" + false + " - " + item.getProperties().toString());
          EventResult eventResult =
              new EventResult(eventQuery.getEventName(), String.valueOf(item.get("hashKey")), null,
                  "{\"result\": false, \"result-event\": {" + item.getProperties().toString()
                      + "}, \"rule\":\"" + eventQuery.getRuleName() + "\"}");
          CassandraRepository.getInstance().updateResults(eventResult);
        } else { // if not exception
          // check condition to get final result
          if (eventQueryCondition != null && !Constants.EMPTY_STRING.equals(eventQueryCondition)) {
            if (!item.getProperties().keySet().isEmpty()) {
              Map<String, Object> properties = item.getProperties();
              Iterator<String> keys = properties.keySet().iterator();
              while (keys.hasNext()) {
                String key = keys.next();
                eventQueryCondition =
                    eventQueryCondition.replace(key, String.valueOf(properties.get(key)));
              }
              ScriptEngineManager mgr = new ScriptEngineManager();
              ScriptEngine engine = mgr.getEngineByName("JavaScript");
              try {
                // check condition for EventQuery
                boolean checked = (Boolean) engine.eval(eventQueryCondition);
                if (checked) {// if violate(meet condition)
                  logger.info("Result:" + checked + " - " + item.getProperties().toString());
                  String jsonStr = JsonHelper.toJSonString(item.getProperties());
                  EventResult eventResult =
                      new EventResult(eventQuery.getEventName(),
                          String.valueOf(item.get("hashKey")), "{\"result\": " + checked
                              + ", \"result-event\": "
                              + jsonStr + ", \"rule\":\""
                              + eventQuery.getRuleName() + "\"}", null);
                  CassandraRepository.getInstance().updateResults(eventResult);
                }
              } catch (Exception e) {
                logger.error(e.getMessage(), e);
                EventResult eventResult =
                    new EventResult(eventQuery.getEventName(), String.valueOf(item.get("hashKey")),
                        "{\"result\": false, \"exception\": \"" + e.getMessage()
                            + "\", \"reult\": \"" + eventQuery.getRuleName() + "\"}", null);
                CassandraRepository.getInstance().updateResults(eventResult);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

}

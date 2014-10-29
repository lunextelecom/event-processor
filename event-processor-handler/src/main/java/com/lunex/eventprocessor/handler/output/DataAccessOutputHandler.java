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

import com.espertech.esper.client.PropertyAccessException;
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
import com.lunex.eventprocessor.core.utils.StringUtils;
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
          CassandraRepository.getInstance(Configurations.cassandraHost,
              Configurations.cassandraKeyspace).insertEventToDB(insertEvent);
          // insert event result --> default no violate
          CassandraRepository.getInstance(Configurations.cassandraHost,
              Configurations.cassandraKeyspace).insertResults(insertEvent.getEvtName(),
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

    Map<String, Object> resultPropeties = null;
    KairosDBClient client = new KairosDBClient(Configurations.kairosDBUrl);
    for (int i = 0; i < result.length; i++) {
      try {
        resultPropeties = (Map<String, Object>) result[i];
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
          if (map.get(key).equals("string")) { // create metric name from fields of event
            Object value = null;
            for (Entry<String, Object> e : resultPropeties.entrySet()) {
              if (e.getKey().indexOf(key) > 0) {
                value = e.getValue();
                client.sendMetric(metric + "."
                    + e.getKey().replace(StringUtils.seperatorField, "."),
                    System.currentTimeMillis(), value, tags);
                break;
              }
            }
          } else {// create and send value from fields of event
            Object value = null;
            for (Entry<String, Object> e : resultPropeties.entrySet()) {
              if (e.getKey().indexOf(key) > 0) {
                value = e.getValue();
                client.sendMetric(metric + "."
                    + e.getKey().replace(StringUtils.seperatorField, "."),
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
   * Write result computation from continuous query to cassandra
   * 
   * @param item
   * @param eventQuery
   * @throws PropertyAccessException
   * @throws Exception
   */
  public static void writeResultComputation(Object[] result, EventQuery eventQuery)
      throws PropertyAccessException, Exception {
    Map<String, Object> item = null;
    String hashKey = null;
    for (int i = 0; i < result.length; i++) {
      item = (Map<String, Object>) result[i];
      if (item == null || item.isEmpty()) {
        continue;
      }
      hashKey =
          String
              .valueOf(item.get("hashKey") == null ? Constants.EMPTY_STRING : item.get("hashKey"));
      if (Constants.EMPTY_STRING.equals(hashKey)) {
        continue;
      }
      item = StringUtils.revertHashMapField(item);
      String jsonStr = JsonHelper.toJSonString(item);
      CassandraRepository.getInstance(Configurations.cassandraHost,
          Configurations.cassandraKeyspace).insertResultComputation(eventQuery.getEventName(),
          eventQuery.getRuleName(), (Long) item.get("time"), String.valueOf(item.get("hashKey")),
          jsonStr);
    }
  }

  /**
   * Write result into cassandra
   * 
   * @param result
   * @param eventQuery
   */
  public static List<EventResult> checkCondition(Object[] result, EventQuery eventQuery,
      CheckConditionHandler checkConditionHandler) {
    if (result == null || result.length == 0) {
      return null;
    }
    List<EventResult> listResult = new ArrayList<EventResult>();
    try {
      // get list condition excption
      List<EventQueryException> condtionExceptions =
          CassandraRepository.getInstance(Configurations.cassandraHost,
              Configurations.cassandraKeyspace).getEventQueyExceptionNotExpired(eventQuery,
              ExptionAction.VERIFIED.toString());

      // String eventQueryCondition = eventQuery.getConditions();
      Map<String, Object> properties = null;
      String hashKey = null;
      for (int i = 0; i < result.length; i++) {
        properties = (Map<String, Object>) result[i];
        hashKey =
            String.valueOf(properties.get("hashKey") == null ? Constants.EMPTY_STRING : properties
                .get("hashKey"));
        if (properties == null || properties.isEmpty() || Constants.EMPTY_STRING.equals(hashKey)) {
          continue;
        }

        // check condition exception
        // write result check violation -> Condition truncate the resulting data into a bool
        // The first: get exception if it exist
        boolean eventException = false;
        if (condtionExceptions != null && condtionExceptions.size() > 0) {
          EventQueryException eventQueryException = null;
          Iterator<String> keys = null;
          String key = null;
          Object exception = null;
          for (int j = 0; j < condtionExceptions.size(); j++) {
            eventQueryException = condtionExceptions.get(j);
            if (eventQueryException.getConditionFilter() == null) {
              continue;
            }
            keys = properties.keySet().iterator();
            int numMappingException = 0;
            key = null;
            exception = null;
            while (keys.hasNext()) {
              key = keys.next();
              exception = eventQueryException.getConditionFilter().get(key);
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

        // if exception is existed
        if (eventException) {
          // result is false (no violate)
          logger.info("Result:" + false + " - " + properties.toString());
          // create result with filter result
          EventResult eventResult =
              new EventResult(eventQuery.getEventName(), hashKey, null,
                  "{\"result\": false, \"result-event\": {" + properties.toString()
                      + "}, \"rule\":\"" + eventQuery.getRuleName() + "\"}");
          listResult.add(eventResult);
        } else { // if not exception is exist
          // check condition to get final result. When this condition is met, check will return true
          // else false
          EventResult eventResult =
              checkConditionHandler.checkCondition(properties, eventQuery, hashKey);
          listResult.add(eventResult);
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return listResult;
  }

}

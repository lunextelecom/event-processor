package com.lunex.eventprocessor.handler.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.event.map.MapEventBean;
import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.dataaccess.KairosDBClient;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.handler.utils.Configurations;

public class DataAccessOutputHandler {
  static final Logger logger = LoggerFactory.getLogger(DataAccessOutputHandler.class);

  /**
   * Save raw event into DB
   * 
   * @param event
   */
  public static void insertRawEvent(Event event) {
    // insert raw event to db
    final Event insertEvent = event;
    Thread insertEventThread = new Thread(new Runnable() {
      public void run() {
        try {
          CassandraRepository.getInstance().insertEventToDB(insertEvent);
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
    });
    insertEventThread.start();
  }

  /**
   * Write result into KairosDB
   * 
   * @param result
   * @param eventQuery
   */
  public static void writeKairosDB(Object[] result, EventQuery eventQuery) {
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

}

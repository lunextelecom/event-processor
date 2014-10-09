package com.lunex.eventprocessor.handler.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.espertech.esper.event.map.MapEventBean;
import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.dataaccess.KairosDBClient;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.handler.utils.Configurations;

public class KairosDBOutput implements ResultListener {

  private QueryFuture queryFuture;

  public void setQueryFuture(QueryFuture queryFuture) {
    this.queryFuture = queryFuture;
  }

  public QueryFuture getQueryFuture() {
    return this.queryFuture;
  }

  public void onEvent(Object[] result) {
    EventQuery eventQuery = null;
    if (queryFuture != null) {
      eventQuery = queryFuture.getEventQuery();
    } else {
      return;
    }

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
          if (!map.get(key).equals("string")) {
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
                client.sendMetric(metric + "." + e.getKey().replace("(", ".").replace("", ""),
                    System.currentTimeMillis(), value, tags);
                break;
              }
            }
          }
        }
      } catch (Exception ex) {
      }
    }
  }
}

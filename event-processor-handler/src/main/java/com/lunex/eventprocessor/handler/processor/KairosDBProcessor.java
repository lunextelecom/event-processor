package com.lunex.eventprocessor.handler.processor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryHierarchy;
import com.lunex.eventprocessor.core.dataaccess.KairosDBClient;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.handler.utils.Configurations;

public class KairosDBProcessor implements Processor {

  static final Logger logger = LoggerFactory.getLogger(KairosDBProcessor.class);

  private QueryHierarchy queryHierarchy;

  public void consume(Event event) {
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

  public QueryHierarchy getHierarchy() {
    return this.queryHierarchy;
  }

  public void setHierarchy(QueryHierarchy hierarchy) {
    this.queryHierarchy = hierarchy;
  }

}

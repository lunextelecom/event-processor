package com.lunex.eventprocessor.handler.listener;

import java.util.Iterator;
import java.util.Map;

import com.espertech.esper.event.map.MapEventBean;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.Constants;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example console output writer
 */
public class ConsoleOutput implements ResultListener {

  static final Logger logger = LoggerFactory.getLogger(ConsoleOutput.class);

  private QueryFuture queryFuture;

  public void setQueryFuture(QueryFuture queryFuture) {
    this.queryFuture = queryFuture;
  }

  public QueryFuture getQueryFuture() {
    return queryFuture;
  }

  public void onEvent(Object[] result) {
    EventQuery eventQuery = null;
    if (queryFuture != null) {
      eventQuery = queryFuture.getEventQuery();
    } else {
      return;
    }
    String eventQueryCondition = eventQuery.getConditions();
    for (int i = 0; i < result.length; i++) {
      MapEventBean item = (MapEventBean) result[i];
      if (eventQueryCondition != null && !Constants.EMPTY_STRING.equals(eventQueryCondition)) {
        if (!item.getProperties().keySet().isEmpty()) {
          Map<String, Object> properties = item.getProperties();
          Iterator<String> keys = item.getProperties().keySet().iterator();
          while (keys.hasNext()) {
            String key = keys.next();
            eventQueryCondition =
                eventQueryCondition.replace(key, String.valueOf(properties.get(key)));
          }
          ScriptEngineManager mgr = new ScriptEngineManager();
          ScriptEngine engine = mgr.getEngineByName("JavaScript");
          try {
            boolean checked = (Boolean) engine.eval(eventQueryCondition);
            System.out.println("Result:" + checked);
            System.out.println(item.getProperties().toString());
          } catch (ScriptException e) {
            logger.error(e.getMessage());
          }
        }
      } else {
        System.out.println(item.getProperties().toString());
      }
    }
  }
}

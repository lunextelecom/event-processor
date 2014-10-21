package com.lunex.eventprocessor.handler.output;

import java.util.Iterator;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.core.utils.StringUtils;

public class CheckConditionDayOfWeek implements CheckConditionHandler {

  static final Logger logger = LoggerFactory.getLogger(CheckConditionDayOfWeek.class);

  public EventResult checkCondition(Map<String, Object> properties, EventQuery eventQuery,
      String hashKey) {
    String eventQueryCondition = eventQuery.getConditions();
    if (eventQueryCondition != null && !Constants.EMPTY_STRING.equals(eventQueryCondition)) {
      if (!properties.keySet().isEmpty()) {
        Iterator<String> keys = properties.keySet().iterator();
        while (keys.hasNext()) {
          String key = keys.next();
          String newkey = StringUtils.revertSingleField(key);
          eventQueryCondition =
              eventQueryCondition.replace(newkey, String.valueOf(properties.get(key)));
        }
        //
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        try {
          // check condition for EventQuery
          boolean checked = (Boolean) engine.eval(eventQueryCondition);
          logger.info("Result:" + checked + " - " + properties.toString());
          properties = StringUtils.revertHashMapField(properties);
          String jsonStr = JsonHelper.toJSonString(properties);
          // create result with filter result is null
          EventResult eventResult =
              new EventResult(eventQuery.getEventName(), String.valueOf(hashKey), "{\"result\": "
                  + checked + ", \"result-event\": " + jsonStr + ", \"rule\":\""
                  + eventQuery.getRuleName() + "\"}", null);
          return eventResult;
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
          // create result with message error
          EventResult eventResult =
              new EventResult(eventQuery.getEventName(), String.valueOf(hashKey),
                  "{\"result\": false, \"exception\": \"" + e.getMessage() + "\", \"reult\": \""
                      + eventQuery.getRuleName() + "\"}", null);
          return eventResult;
        }
      }
    }
    return null;
  }

}

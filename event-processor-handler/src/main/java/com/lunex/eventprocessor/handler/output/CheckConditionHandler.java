package com.lunex.eventprocessor.handler.output;

import java.util.Map;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventResult;


public interface CheckConditionHandler {

  public EventResult checkCondition(Map<String, Object> properties, EventQuery eventQuery, String hashKey);
}

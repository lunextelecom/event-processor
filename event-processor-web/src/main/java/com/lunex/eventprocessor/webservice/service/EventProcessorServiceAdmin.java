package com.lunex.eventprocessor.webservice.service;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventQueryException;
import com.lunex.eventprocessor.core.EventQuery.EventQueryStatus;
import com.lunex.eventprocessor.core.EventQueryException.ExptionAction;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.core.utils.TimeUtil;

public class EventProcessorServiceAdmin {

  private CassandraRepository cassandraRepository;

  public EventProcessorServiceAdmin(CassandraRepository cassandraRepository) {
    this.cassandraRepository = cassandraRepository;
  }

  public void addRuleException(String eventName, String ruleName, String action, String datetinme,
      String filter) throws Exception {
    Map<String, Object> map = new HashMap<String, Object>();
    map = JsonHelper.toMap(new JSONObject(filter));
    EventQueryException eventQueyException =
        new EventQueryException(eventName, ruleName, ExptionAction.valueOf(action),
            TimeUtil.convertStringToDate(datetinme, "dd/MM/yyyy HH:mm:ss"), map);
    cassandraRepository.insertEventQueryException(eventQueyException);
  }

  public void addRule(String eventName, String ruleName, String data, String fields,
      String filters, String aggregateField, String having, String smallBucket, String bigBucket,
      String conditions, String description) throws Exception {
    EventQuery eventQuery = new EventQuery();
    eventQuery.setEventName(eventName);
    eventQuery.setRuleName(ruleName);
    eventQuery.setData(data);
    eventQuery.setFields(fields);
    eventQuery.setFilters(filters);
    eventQuery.setAggregateField(aggregateField);
    eventQuery.setSmallBucket(smallBucket);
    eventQuery.setBigBucket(bigBucket);
    eventQuery.setConditions(conditions);
    eventQuery.setHaving(having);
    eventQuery.setDescription(description);
    eventQuery.setStatus(EventQueryStatus.STOP);
    cassandraRepository.insertEventQuery(eventQuery);
  }
  
  public void deleteRule(String eventName, String ruleName) throws Exception {
    EventQuery eventQuery = new EventQuery();
    eventQuery.setEventName(eventName);
    eventQuery.setRuleName(ruleName);
    cassandraRepository.deleteEventQuery(eventQuery);
  }
}

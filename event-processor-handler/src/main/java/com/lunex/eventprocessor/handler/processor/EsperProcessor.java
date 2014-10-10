package com.lunex.eventprocessor.handler.processor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.QueryHierarchy;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;

public class EsperProcessor implements Processor {

  static final Logger logger = LoggerFactory.getLogger(EsperProcessor.class);

  private QueryHierarchy queryHierarchy;
  private EPServiceProvider sericeProvider;

  public EsperProcessor(List<EventProperty> eventProperty, List<EventQuery> listEventQuery) {
    Configuration config = new Configuration();
    EventProperty propeties = null;
    for (int i = 0, size = eventProperty.size(); i < size; i++) {
      propeties = eventProperty.get(i);
      propeties.getProperties().put("hashKey", "string");
      config.addEventType(propeties.getEvtDataName(), propeties.getProperties());
    }
    sericeProvider = EPServiceProviderManager.getProvider("event-processor-engine", config);

    EPAdministrator admin = sericeProvider.getEPAdministrator();
    for (int i = 0, size = listEventQuery.size(); i < size; i++) {
      final EventQuery eventQuery = listEventQuery.get(i);
      EventQuery newEventQuery = EventQueryProcessor.processEventQuery(eventQuery);
      String timeSeries =
          (Constants.EMPTY_STRING.equals(eventQuery.getTimeSeries())) ? "" : ".win:time("
              + newEventQuery.getTimeSeries() + ")";

      EPStatement statement =
          admin.createEPL(String.format(
              "SELECT %s,hashKey FROM %s%s WHERE %s %s %s",
              newEventQuery.getFields(),
              newEventQuery.getData(),
              timeSeries,
              newEventQuery.getFilters(),
              (newEventQuery.getAggregateField() == null || Constants.EMPTY_STRING
                  .equals(newEventQuery.getAggregateField())) ? "" : "GROUP BY "
                  + newEventQuery.getAggregateField(),
              (newEventQuery.getHaving() == null || Constants.EMPTY_STRING.equals(newEventQuery
                  .getHaving())) ? "" : "HAVING " + newEventQuery.getHaving()));

      statement.addListener(new UpdateListener() {
        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
          // TODO: trigger event and process
          QueryFuture queryFuture = new QueryFuture(newEvents, eventQuery);
          ResultListener[] listener =
              queryHierarchy.getHierarchy().get(eventQuery.getEventName()).get(eventQuery);
          queryHierarchy.bindOutput(queryFuture, listener);
        }
      });
    }
  }

  public void consume(Event event) {
    if (event == null) {
      logger.error("Event is null");
      return;
    }
    logger.info("Start consume event:" + event.toString());
    DataAccessOutputHandler.insertRawEventToCassandra(event);
    sericeProvider.getEPRuntime().sendEvent(event.getEvent(), event.getEvtName());
  }

  public QueryHierarchy getHierarchy() {
    return queryHierarchy;
  }

  public void setHierarchy(QueryHierarchy hierarchy) {
    this.queryHierarchy = hierarchy;
  }
}

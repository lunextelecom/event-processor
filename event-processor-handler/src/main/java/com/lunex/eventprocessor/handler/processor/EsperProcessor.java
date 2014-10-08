package com.lunex.eventprocessor.handler.processor;

import java.util.List;

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
import com.lunex.eventprocessor.core.utils.Constants;

public class EsperProcessor implements Processor {

  private QueryHierarchy queryHierarchy;
  private EPServiceProvider sericeProvider;

  public EsperProcessor(List<EventProperty> eventProperty, List<EventQuery> listEventQuery) {
    Configuration config = new Configuration();
    EventProperty propeties = null;
    for (int i = 0, size = eventProperty.size(); i < size; i++) {
      propeties = eventProperty.get(i);
      config.addEventType(propeties.getEvtDataName(), propeties.getProperties());
    }
    sericeProvider = EPServiceProviderManager.getProvider("event-processor-engine", config);

    EPAdministrator admin = sericeProvider.getEPAdministrator();
    for (int i = 0, size = listEventQuery.size(); i < size; i++) {
      final EventQuery eventQuery = listEventQuery.get(i);
      String timeSeries =
          (Constants.EMPTY_STRING.equals(eventQuery.getTimeSeries())) ? "" : ".win:time("
              + eventQuery.getTimeSeries() + ")";

      EPStatement statement =
          admin.createEPL(String.format("SELECT %s FROM %s%s WHERE %s GROUP BY %s HAVING %s",
              eventQuery.getFields(), eventQuery.getData(), timeSeries, eventQuery.getFilters(),
              eventQuery.getAggregateField(), eventQuery.getHaving()));

      statement.addListener(new UpdateListener() {
        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
          // TODO: trigger event and process
          QueryFuture queryFuture = new QueryFuture(newEvents, eventQuery);
        }
      });
    }
  }

  public void consume(Event event) {
    System.out.println("Start consume event:" + event.toString());
    sericeProvider.getEPRuntime().sendEvent(event.getEvent(), event.getEvtName());
  }

  public QueryHierarchy getHierarchy() {
    return queryHierarchy;
  }

  public void setHierarchy(QueryHierarchy hierarchy) {
    this.queryHierarchy = hierarchy;
  }
}

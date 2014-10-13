package com.lunex.eventprocessor.handler.processor;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.QueryFuture;
import com.lunex.eventprocessor.core.QueryHierarchy;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;

public class EsperProcessor implements Processor {

  static final Logger logger = LoggerFactory.getLogger(EsperProcessor.class);

  private QueryHierarchy queryHierarchy;
  private EPServiceProvider sericeProvider;

  public EsperProcessor(List<EventProperty> eventProperty, List<EventQuery> listEventQuery) {
    try {
      this.intiConfig(eventProperty);
      this.initEPLNoBackFill(listEventQuery, false, System.currentTimeMillis());
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
    }
  }

  public void consume(Event event) {
    if (event == null) {
      logger.error("Event is null");
      return;
    }
    logger.info("Start consume event:" + event.toString());
    // save raw event
    DataAccessOutputHandler.insertRawEventToCassandra(event);

    // Process send event to esper
    // move forward time by event Time
    sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(event.getTime()));
    // send event
    sericeProvider.getEPRuntime().sendEvent(event.getEvent(), event.getEvtName());
  }

  public QueryHierarchy getHierarchy() {
    return queryHierarchy;
  }

  public void setHierarchy(QueryHierarchy hierarchy) {
    this.queryHierarchy = hierarchy;
  }

  /**
   * Init config for Esper
   * 
   * @param eventProperty
   * @param startTime
   */
  private void intiConfig(List<EventProperty> eventProperty) {
    Configuration config = new Configuration();
    EventProperty propeties = null;
    for (int i = 0, size = eventProperty.size(); i < size; i++) {
      propeties = eventProperty.get(i);
      propeties.getProperties().put("hashKey", "string");
      propeties.getProperties().put("time", "long");
      config.addEventType(propeties.getEvtDataName(), propeties.getProperties());
    }
    config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
    // config.getEngineDefaults().getViewResources().setShareViews(false);
    sericeProvider = EPServiceProviderManager.getProvider("event-processor-engine", config);
  }

  /**
   * Create EPL to start process(no backfill)
   * 
   * @param listEventQuery -> list Event Query to build EPL
   * @param backFill: true: process -> backfill, false -> no process backfill
   * @param startTime: time to determine how far to backfill, if backFill is false --> default is
   *        system current time
   * @throws Exception
   */
  public void initEPLNoBackFill(List<EventQuery> listEventQuery, boolean backFill, long startTime)
      throws Exception {
    // set start time when start esper
    if (backFill == true) {
      sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime));
    } else {
      sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(System.currentTimeMillis()));
    }
    EPAdministrator admin = sericeProvider.getEPAdministrator();
    for (int i = 0, size = listEventQuery.size(); i < size; i++) {
      final EventQuery eventQuery = listEventQuery.get(i);
      EventQuery newEventQuery = EventQueryProcessor.processEventQuery(eventQuery);
      String timeSeries =
          (Constants.EMPTY_STRING.equals(eventQuery.getTimeSeries())) ? "" : ".win:time("
              + newEventQuery.getTimeSeries() + ")";

      String epl =
          String.format("SELECT %s,hashKey,time FROM %s%s %s %s %s", newEventQuery.getFields(),
              newEventQuery.getData(), timeSeries,
              (newEventQuery.getFilters() == null || Constants.EMPTY_STRING.equals(newEventQuery
                  .getFilters())) ? "" : "WHERE " + newEventQuery.getFilters(), (newEventQuery
                  .getAggregateField() == null || Constants.EMPTY_STRING.equals(newEventQuery
                  .getAggregateField())) ? "" : "GROUP BY " + newEventQuery.getAggregateField(),
              (newEventQuery.getHaving() == null || Constants.EMPTY_STRING.equals(newEventQuery
                  .getHaving())) ? "" : "HAVING " + newEventQuery.getHaving());
      EPStatement statement = admin.createEPL(epl);

      statement.addListener(new UpdateListener() {
        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
          // TODO: trigger event and process
          if (newEvents == null || newEvents.length == 0) {
            return;
          }
          QueryFuture queryFuture = new QueryFuture(newEvents, eventQuery);
          String eventName = eventQuery.getEventName();
          Map<EventQuery, ResultListener[]> mapResultListener =
              queryHierarchy.getHierarchy().get(eventName);
          if (mapResultListener != null) {
            ResultListener[] listener = mapResultListener.get(eventQuery);
            if (listener != null && listener.length > 0) {
              queryHierarchy.bindOutput(queryFuture, listener);
            }
          }
        }
      });
    }

    // if this init is backfill mode
    if (backFill) {
      if (startTime == -1) {
        logger.error("No define how far to backfill");
        return;
      }
      this.feedHistoricalEvent(startTime);
    }
  }

  public void feedHistoricalEvent(long startTime) throws Exception {
    // get historical event from DB
    List<Event> listEvent = CassandraRepository.getInstance().getEvent(startTime);
    Event historicalEvent = null;
    for (int i = 0, size = listEvent.size(); i < size; i++) {
      historicalEvent = listEvent.get(i);
      // move forward time by event Time
      sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(historicalEvent.getTime()));
      // send event
      sericeProvider.getEPRuntime().sendEvent(historicalEvent.getEvent(),
          historicalEvent.getEvtName());
    }
  }

  /**
   * Update existed EventType for Esper at runtime
   * 
   * @param propeties
   */
  public void updateEsperEventTypeOnRuntime(EventProperty propeties) {
    EPAdministrator admin = this.sericeProvider.getEPAdministrator();
    propeties.getProperties().put("hashKey", "string");
    propeties.getProperties().put("time", "long");
    admin.getConfiguration().updateMapEventType(propeties.getEvtDataName(),
        propeties.getProperties());
  }

  /**
   * Add new EventType for Esper at runtime
   * 
   * @param propeties
   */
  public void addEsperContenTypeOnRunTime(EventProperty propeties) {
    EPAdministrator admin = this.sericeProvider.getEPAdministrator();
    propeties.getProperties().put("hashKey", "string");
    propeties.getProperties().put("time", "long");
    admin.getConfiguration().addEventType(propeties.getEvtDataName(), propeties.getProperties());
  }

  public void addHistoryEvent(long backfillTime, List<Event> listHistoryEvent) {
    // create a isolated statment
    EPServiceProviderIsolated isolatedService =
        sericeProvider.getEPServiceIsolated("suspendedStmts");
    isolatedService.getEPRuntime().sendEvent(new CurrentTimeEvent(backfillTime));

    Event historyEvent = null;
    for (int i = 0, size = listHistoryEvent.size(); i < size; i++) {
      historyEvent = listHistoryEvent.get(i);
      if (historyEvent.getTime() < backfillTime) {
        continue;
      }
      isolatedService.getEPRuntime().sendEvent(new CurrentTimeEvent(historyEvent.getTime()));
      isolatedService.getEPRuntime().sendEvent(historyEvent);
      // repeat the above advancing time until no more events
    }
    // isolatedService.getEPAdministrator().re
  }
}

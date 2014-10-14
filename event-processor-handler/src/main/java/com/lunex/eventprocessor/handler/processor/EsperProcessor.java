package com.lunex.eventprocessor.handler.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.event.map.MapEventBean;
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

  public EsperProcessor(QueryHierarchy queryHierarchy, List<EventProperty> eventProperty,
      List<EventQuery> listEventQuery, boolean backFill, long startTime) {
    try {
      this.queryHierarchy = queryHierarchy;
      this.intiConfig(eventProperty);
      this.initEPL(listEventQuery, backFill, startTime);
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
    this.sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(event.getTime()));
    // send event
    this.sericeProvider.getEPRuntime().sendEvent(event.getEvent(), event.getEvtName());
  }

  public QueryHierarchy getHierarchy() {
    return this.queryHierarchy;
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
    // detroy firstly to reset config
    if (this.sericeProvider != null) {
      this.sericeProvider.destroy();
    }

    // add new config
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
    this.sericeProvider = EPServiceProviderManager.getProvider("event-processor-engine", config);
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
  public void initEPL(List<EventQuery> listEventQuery, boolean backFill, long startTime)
      throws Exception {
    // detroy all statement firstly to reset
    this.sericeProvider.getEPAdministrator().destroyAllStatements();

    // set start time when start esper
    if (backFill == true) {
      this.sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime));
    } else {
      this.sericeProvider.getEPRuntime()
          .sendEvent(new CurrentTimeEvent(System.currentTimeMillis()));
    }
    // Create EPL
    EPAdministrator admin = sericeProvider.getEPAdministrator();
    for (int i = 0, size = listEventQuery.size(); i < size; i++) {
      final EventQuery eventQuery = listEventQuery.get(i);
      EventQuery newEventQuery = EventQueryProcessor.processEventQuery(eventQuery);
      String timeSeries =
          (Constants.EMPTY_STRING.equals(eventQuery.getSmallBucket())) ? "" : ".win:time("
              + newEventQuery.getSmallBucket() + ")";

      String epl =
          String.format("SELECT %s, hashKey, time FROM %s%s %s %s %s", newEventQuery.getFields(),
              newEventQuery.getData(), timeSeries,
              (newEventQuery.getFilters() == null || Constants.EMPTY_STRING.equals(newEventQuery
                  .getFilters())) ? "" : "WHERE " + newEventQuery.getFilters(), (newEventQuery
                  .getAggregateField() == null || Constants.EMPTY_STRING.equals(newEventQuery
                  .getAggregateField())) ? "" : "GROUP BY " + newEventQuery.getAggregateField(),
              (newEventQuery.getHaving() == null || Constants.EMPTY_STRING.equals(newEventQuery
                  .getHaving())) ? "" : "HAVING " + newEventQuery.getHaving());
      EPStatement statement = admin.createEPL(epl);
      // Add listener, default listener is enable = false
      statement.addListener(new EsperListener(eventQuery));
    }

    // if this init is backfill mode
    if (backFill) {
      if (startTime == -1) {
        logger.error("No define how far to backfill");
        return;
      }
      this.feedHistoricalEvent(startTime);
    }

    // start listener enable = true
    this.startListener();
  }

  /**
   * Start listener for Esper
   */
  public void startListener() {
    EPAdministrator admin = sericeProvider.getEPAdministrator();
    String[] statements = admin.getStatementNames();
    EPStatement statement = null;
    EsperListener lisnter = null;
    for (int i = 0, length = statements.length; i < length; i++) {
      statement = admin.getStatement(statements[i]);
      if (statement.getUpdateListeners().hasNext()) {
        lisnter = (EsperListener) statement.getUpdateListeners().next();
        lisnter.setEnable(true);
      }
    }
    System.out.println("EPL is ready");
  }

  /**
   * Send historical Event to Esper
   * 
   * @param startTime
   * @throws Exception
   */
  public void feedHistoricalEvent(long startTime) throws Exception {
    // get historical event from DB
    List<Event> listEvent = CassandraRepository.getInstance().getEvent(startTime);
    if (listEvent != null) {
      Event historicalEvent = null;
      for (int i = 0, size = listEvent.size(); i < size; i++) {
        historicalEvent = listEvent.get(i);
        // consume event but not process listener
        this.consume(historicalEvent);
      }
    }
  }

  /**
   * Update existed EventType for Esper at runtime
   * 
   * @param propeties
   */
  public void updateEsperEventTypeOnRuntime(EventProperty propeties) {
    if (this.sericeProvider == null) {
      return;
    }
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
    if (this.sericeProvider == null) {
      return;
    }
    EPAdministrator admin = this.sericeProvider.getEPAdministrator();
    propeties.getProperties().put("hashKey", "string");
    propeties.getProperties().put("time", "long");
    admin.getConfiguration().addEventType(propeties.getEvtDataName(), propeties.getProperties());
  }

  /**
   * Class process listener for esper
   *
   */
  public class EsperListener implements UpdateListener {

    private EventQuery eventQuery;

    private boolean enable = false;

    public EsperListener(EventQuery eventQuery) {
      this.eventQuery = eventQuery;
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
      if (newEvents == null || newEvents.length == 0 || !enable) {
        return;
      }
      List<Map<String, Object>> objectArray = new ArrayList<Map<String, Object>>();
      for (int i = 0; i < newEvents.length; i++) {
        MapEventBean eventbean = (MapEventBean) newEvents[i];
        objectArray.add(eventbean.getProperties());
      }
      QueryFuture queryFuture = new QueryFuture(objectArray.toArray(), eventQuery);
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

    public boolean isEnable() {
      return enable;
    }

    public void setEnable(boolean enable) {
      this.enable = enable;
    }

  }
}

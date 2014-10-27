package com.lunex.eventprocessor.handler.processor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPRuntimeIsolated;
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
import com.lunex.eventprocessor.core.EventQuery.EventQueryStatus;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.listener.ResultListener;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.EventQueryProcessor;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.handler.listener.CassandraWriter;
import com.lunex.eventprocessor.handler.listener.ConsoleOutput;
import com.lunex.eventprocessor.handler.listener.KafkaWriter;
import com.lunex.eventprocessor.handler.listener.KairosDBWriter;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;
import com.lunex.eventprocessor.handler.utils.Configurations;

public class EsperProcessor implements Processor {

  static final Logger logger = LoggerFactory.getLogger(EsperProcessor.class);

  private QueryHierarchy queryHierarchy;
  private Map<String, EPServiceProvider> mapServiceProvider;

  public EsperProcessor(QueryHierarchy queryHierarchy, List<EventProperty> eventProperty,
      List<EventQuery> listEventQuery, boolean backFill, long startTime) {
    try {
      mapServiceProvider = new HashMap<String, EPServiceProvider>();
      this.queryHierarchy = queryHierarchy;
      Configuration config = this.intiConfig(eventProperty);
      this.initEPL(config, listEventQuery, backFill, startTime);
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
    Iterator<String> iterator = mapServiceProvider.keySet().iterator();
    while (iterator.hasNext()) {
      String key = iterator.next();
      EPServiceProvider sericeProvider = mapServiceProvider.get(key);
      sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(event.getTime()));
      // send event
      System.out.println("Send event " + event.getEvent() + " - " + new Date());
      sericeProvider.getEPRuntime().sendEvent(event.getEvent(), event.getEvtName());
      // forward time
      sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(event.getTime() + 1001));
    }
  }

  public QueryHierarchy getHierarchy() {
    return this.queryHierarchy;
  }

  public void setHierarchy(QueryHierarchy hierarchy) {
    this.queryHierarchy = hierarchy;
  }

  public boolean updateRule(EventQuery eventQuery, boolean backfill, long backFillTime) {
    String eventName = eventQuery.getEventName();
    String ruleName = eventQuery.getRuleName();

    // stop and destroy
    String serviceProviderURI = eventName + ":" + ruleName;
    EPServiceProvider serviceProvider = this.mapServiceProvider.get(serviceProviderURI);
    if (serviceProvider == null) {
      return false;
    }
    this.mapServiceProvider.remove(serviceProviderURI);
    this.queryHierarchy.removeQueryHierarchy(eventName, eventQuery);
    serviceProvider.destroy();

    try {
      // Create EPServiceProvider
      List<EventProperty> temp = new ArrayList<EventProperty>();
      temp.add(EventQueryProcessor.processEventProperyForEventQuery(eventQuery));
      Configuration config = intiConfig(temp);
      serviceProvider = this.createEPServiceProvider(config, eventQuery, backfill, backFillTime);
      // Add to Map and run
      this.mapServiceProvider.put(serviceProviderURI, serviceProvider);
      this.queryHierarchy.addQuery(eventName, eventQuery, new ResultListener[] {
          new ConsoleOutput(), new CassandraWriter(), new KairosDBWriter(), new KafkaWriter()});
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return false;
    }
    return true;
  }

  public boolean startRule(EventQuery eventQuery, boolean backfill, long backFillTime) {
    try {
      String eventName = eventQuery.getEventName();
      String ruleName = eventQuery.getRuleName();
      String serviceProviderURI = eventName + ":" + ruleName;
      // Create EPServiceProvider
      List<EventProperty> temp = new ArrayList<EventProperty>();
      temp.add(EventQueryProcessor.processEventProperyForEventQuery(eventQuery));
      Configuration config = intiConfig(temp);
      EPServiceProvider serviceProvider =
          this.createEPServiceProvider(config, eventQuery, backfill, backFillTime);
      // Add to Map
      this.mapServiceProvider.put(serviceProviderURI, serviceProvider);
      // Add to QueryHierarchy
      this.queryHierarchy.addQuery(eventName, eventQuery, new ResultListener[] {
          new ConsoleOutput(), new CassandraWriter(), new KairosDBWriter(), new KafkaWriter()});
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return false;
    }
    return true;
  }

  public boolean stopRule(EventQuery eventQuery) {
    String eventName = eventQuery.getEventName();
    String ruleName = eventQuery.getRuleName();
    String serviceProviderURI = eventName + ":" + ruleName;
    // Get EPServiceProvider from Map
    EPServiceProvider serviceProvider = this.mapServiceProvider.get(serviceProviderURI);
    if (serviceProvider == null) {
      return false;
    }
    // Remove from Map
    this.mapServiceProvider.remove(serviceProviderURI);
    // and detroy
    serviceProvider.destroy();
    // Remove from QueryHierarchy
    this.queryHierarchy.removeQueryHierarchy(eventName, eventQuery);
    return true;
  }

  public boolean reprocess(EventQuery eventQuery, boolean backfill, long backFillTime) {
    String eventName = eventQuery.getEventName();
    String ruleName = eventQuery.getRuleName();

    String serviceProviderURI = eventName + ":" + ruleName;
    EPServiceProvider serviceProvider = this.mapServiceProvider.get(serviceProviderURI);
    if (serviceProvider == null) {
      return false;
    }
    this.mapServiceProvider.remove(serviceProviderURI);
    this.queryHierarchy.removeQueryHierarchy(eventName, eventQuery);
    serviceProvider.destroy();

    try {
      // Create EPServiceProvider
      List<EventProperty> temp = new ArrayList<EventProperty>();
      temp.add(EventQueryProcessor.processEventProperyForEventQuery(eventQuery));
      Configuration config = intiConfig(temp);
      serviceProvider = this.createEPServiceProvider(config, eventQuery, backfill, backFillTime);
      // Add to Map
      this.mapServiceProvider.put(serviceProviderURI, serviceProvider);
      this.queryHierarchy.addQuery(eventName, eventQuery, new ResultListener[] {
          new ConsoleOutput(), new CassandraWriter(), new KairosDBWriter(), new KafkaWriter()});
      this.backfill(serviceProvider, backFillTime, backfill);
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return false;
    }
    return true;
  }

  /**
   * Init config for Esper
   * 
   * @param eventProperty
   * @param startTime
   */
  private Configuration intiConfig(List<EventProperty> eventProperty) {
    // create new config
    Configuration config = new Configuration();
    EventProperty propeties = null;
    for (int i = 0, size = eventProperty.size(); i < size; i++) {
      propeties = eventProperty.get(i);
      propeties.getProperties().put("hashKey", "string");
      propeties.getProperties().put("time", "long");
      config.addEventType(propeties.getEvtDataName(), propeties.getProperties());
    }
    config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
    config.getEngineDefaults().getThreading().setInternalTimerMsecResolution(1000);
    return config;
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
  public void initEPL(Configuration config, List<EventQuery> listEventQuery, boolean backFill,
      long startTime) throws Exception {
    // --------------------------------------------
    // ----------- Creaet EPL --------------------
    // --------------------------------------------
    for (int i = 0, size = listEventQuery.size(); i < size; i++) {

      final EventQuery eventQuery = listEventQuery.get(i);
      // filter rule by config
      if (Configurations.ruleList != null && !Configurations.ruleList.isEmpty()
          && !Configurations.ruleList.contains(eventQuery.getRuleName())) {
        continue;
      }
      String serviceProviderURI = eventQuery.getEventName() + ":" + eventQuery.getRuleName();
      EPServiceProvider serviceProvider =
          this.createEPServiceProvider(config, eventQuery, backFill, startTime);
      mapServiceProvider.put(serviceProviderURI, serviceProvider);
      eventQuery.setStatus(EventQueryStatus.RUNNING);
      CassandraRepository.getInstance(Configurations.cassandraHost,
          Configurations.cassandraKeyspace).changeEventQueryStatus(eventQuery);
    }
  }

  /**
   * Create EPServiceProvider runtime from EventQuery
   * 
   * @param eventQuery
   * @param backFill
   * @param startTime
   * @return
   * @throws Exception
   */
  private EPServiceProvider createEPServiceProvider(Configuration config, EventQuery eventQuery,
      boolean backFill, long startTime) throws Exception {
    // create new EventQuery after process string of filter, data, field, group...
    EventQuery newEventQuery = EventQueryProcessor.processEventQuery(eventQuery);

    // create EPServiceProvider for EventQuery
    String serviceProviderURI = eventQuery.getEventName() + ":" + eventQuery.getRuleName();
    EPServiceProvider serviceProvider =
        EPServiceProviderManager.getProvider(serviceProviderURI, config);
    EPAdministrator admin = serviceProvider.getEPAdministrator();
    admin.destroyAllStatements();
    // Control timer start timer for esper
    EPRuntime epRuntime = serviceProvider.getEPRuntime();
    epRuntime.sendEvent(new CurrentTimeEvent(0));

    // Create statement
    String smallBucket = newEventQuery.getSmallBucket();
    String bigBucket = newEventQuery.getBigBucket();
    String select = newEventQuery.getFields();
    String from = newEventQuery.getData();
    String where =
        (newEventQuery.getFilters() == null || Constants.EMPTY_STRING.equals(newEventQuery
            .getFilters())) ? "" : "WHERE " + newEventQuery.getFilters();
    String group =
        (newEventQuery.getAggregateField() == null || Constants.EMPTY_STRING.equals(newEventQuery
            .getAggregateField())) ? "" : "GROUP BY " + newEventQuery.getAggregateField();
    String having =
        (newEventQuery.getHaving() == null || Constants.EMPTY_STRING.equals(newEventQuery
            .getHaving())) ? "" : "HAVING " + newEventQuery.getHaving();
    String epl = Constants.EMPTY_STRING;
    // EPL for window last timeframe
    if (bigBucket != null && smallBucket != null && !Constants.EMPTY_STRING.equals(bigBucket)
        && !Constants.EMPTY_STRING.equals(smallBucket)) {

      String tempTable = StringUtils.randomString(10);
      String context = tempTable + "_Per_" + smallBucket.replace(" ", "_");
      String smallBucketWindow = tempTable + "_" + smallBucket.replace(" ", "_");
      // create EPL for context
      epl = "create context " + context + " start @now end after " + smallBucket;
      epl = epl.replaceAll(" +", " ");
      admin.createEPL(epl);
      // create smallbucket aggregation
      epl =
          String
              .format(
                  "context "
                      + context
                      + " insert into "
                      + smallBucketWindow
                      + " SELECT %s, hashKey as hashKey, time as time FROM %s %s %s %s output snapshot when terminated",
                  StringUtils.convertField(select), from, where, group, having);
      epl = epl.replaceAll(" +", " ");
      admin.createEPL(epl);
      // create EPL for big bucket and add listener for statement
      epl =
          " " + "select " + StringUtils.convertField2(select)
              + ", hashKey as hashKey, time as time FROM " + smallBucketWindow + ".win:ext_timed(time, "
              + bigBucket + ") " + group;
      epl = epl.replaceAll(" +", " ");
      EPStatement statement = admin.createEPL(epl, eventQuery.getRuleName());
      // Add listener, default listener is enable = false
      statement.addListener(new EsperListener(eventQuery));


      // EPL for window every timeframe
    } else if ((bigBucket == null || Constants.EMPTY_STRING.equals(bigBucket))
        && (smallBucket != null && !Constants.EMPTY_STRING.equals(smallBucket))) {
      String tempTable = StringUtils.randomString(10);
      String context = tempTable + "_Per_" + smallBucket.replaceAll(" |:", "_");
      List<String> crontabs = StringUtils.convertCrontab(smallBucket);
      if (crontabs.size() == 2) {
        smallBucket = smallBucket.substring(0, smallBucket.indexOf(":"));
        epl =
            "create context " + context + " initiated by @now and pattern [every timer:at("
                + crontabs.get(0) + ")] terminated by pattern [every timer:at(" + crontabs.get(1)
                + ")]";
      } else {
        epl =
            "create context " + context + " initiated by @now and pattern [every timer:interval("
                + smallBucket + ")] terminated after " + smallBucket + "";
      }
      epl = epl.replaceAll(" +", " ");
      admin.createEPL(epl);
      // create smallbucket aggregation
      epl =
          String
              .format(
                  "context "
                      + context
                      + " SELECT %s, hashKey as hashKey, time as time FROM %s %s %s %s output last every 1 second ",
                  StringUtils.convertField(select), from + ".win:ext_timed(time, " + smallBucket
                      + ")", where, group, having);
      epl = epl.replaceAll(" +", " ");
      EPStatement statement = admin.createEPL(epl, eventQuery.getRuleName());
      statement.addListener(new EsperListener(eventQuery));
    } else {
      // TODO nothing to do to create EPL for this rule, this rule is invalid
    }

    this.backfill(serviceProvider, startTime, backFill);
    // ---------------------------------------
    // start listener enable = true
    // ---------------------------------------
    this.startListener(serviceProvider);
    return serviceProvider;
  }

  /**
   * Backfill with history event
   * 
   * @param serviceProvider
   * @param startTime
   * @param backFill
   * @throws Exception
   */
  private void backfill(EPServiceProvider serviceProvider, long startTime, boolean backFill)
      throws Exception {
    EPRuntime epRuntime = serviceProvider.getEPRuntime();
    if (backFill == true) {
      if (startTime == -1) {
        logger.error("No define how far to backfill");
      } else {
        // ---------------------------------------
        // Backfill with history event
        // ---------------------------------------
        long lastEventTime = this.feedHistoricalEvent(startTime, null, epRuntime);

        // forward time
        epRuntime.sendEvent(new CurrentTimeEvent(lastEventTime + 1001));
      }
    }
  }

  /**
   * Start listener for Esper
   */
  public void startListener(EPServiceProvider serviceProvider) {
    EPAdministrator admin = serviceProvider.getEPAdministrator();
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
    logger.info("EPL is ready");
  }

  /**
   * Send historical Event to Esper
   * 
   * @param startTime
   * @throws Exception
   */
  private long feedHistoricalEvent(long startTime, EPRuntimeIsolated runtimIsolated,
      EPRuntime epRunTime) throws Exception {
    // get historical event from DB
    List<Event> listEvent =
        CassandraRepository.getInstance(Configurations.cassandraHost,
            Configurations.cassandraKeyspace).getEvent(startTime);
    if (listEvent != null) {
      Event historicalEvent = null;
      for (int i = 0, size = listEvent.size(); i < size; i++) {
        historicalEvent = listEvent.get(i);
        // consume event but not process listener
        if (historicalEvent == null) {
          logger.error("Event is null");
          return 0;
        }
        // Process send event to esper
        // move forward time by event Time
        epRunTime.sendEvent(new CurrentTimeEvent(historicalEvent.getTime()));
        // send event
        System.out.println("Send event " + historicalEvent.getEvent() + " - " + new Date());
        epRunTime.sendEvent(historicalEvent.getEvent(), historicalEvent.getEvtName());
        if (i == size - 1) {
          return historicalEvent.getTime();
        }
      }
    }
    return 0;
  }

  /**
   * Class process listener for esper
   *
   */
  private class EsperListener implements UpdateListener {

    private EventQuery eventQuery;

    private boolean enable = false;

    public EsperListener(EventQuery eventQuery) {
      this.eventQuery = eventQuery;
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
      // System.out.println("test:" + new Date() + " rule:" + eventQuery.getRuleName() +
      // "-newEvents:"
      // + newEvents[0].getUnderlying());
      if (newEvents == null || newEvents.length == 0 || !enable) {
        return;
      }
      // get result from esper
      List<Map<String, Object>> objectArray = new ArrayList<Map<String, Object>>();
      for (int i = 0, length = newEvents.length; i < length; i++) {
        MapEventBean eventbean = (MapEventBean) newEvents[i];
        objectArray.add(eventbean.getProperties());
      }
      QueryFuture queryFuture = new QueryFuture(objectArray.toArray(), eventQuery);
      String eventName = eventQuery.getEventName();

      // bind out for listeners
      Map<EventQuery, ResultListener[]> mapResultListener =
          queryHierarchy.getHierarchy().get(eventName);
      if (mapResultListener != null) {
        ResultListener[] listener = mapResultListener.get(eventQuery);
        if (listener != null && listener.length > 0) {
          queryHierarchy.bindOutput(queryFuture, listener);
        }
      }
    }

    public void setEnable(boolean enable) {
      this.enable = enable;
    }
  }
}

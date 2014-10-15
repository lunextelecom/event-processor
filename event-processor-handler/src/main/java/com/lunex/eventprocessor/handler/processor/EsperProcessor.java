package com.lunex.eventprocessor.handler.processor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPRuntimeIsolated;
import com.espertech.esper.client.EPServiceProvider;
// import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.client.time.TimerControlEvent.ClockType;
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
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;

public class EsperProcessor implements Processor {

  static final Logger logger = LoggerFactory.getLogger(EsperProcessor.class);

  private QueryHierarchy queryHierarchy;
  private EPServiceProvider sericeProvider;
  List<EPStatement> listStatement = new ArrayList<EPStatement>();

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
    this.sericeProvider.getEPRuntime().sendEvent(new TimerControlEvent(ClockType.CLOCK_EXTERNAL));
    this.sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(event.getTime()));
    // send event
    System.out.println("Send event " + event.getEvent() + " - " + new Date());
    this.sericeProvider.getEPRuntime().sendEvent(event.getEvent(), event.getEvtName());
    this.sericeProvider.getEPRuntime().sendEvent(new TimerControlEvent(ClockType.CLOCK_INTERNAL));
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
    config.getEngineDefaults().getThreading().setInternalTimerMsecResolution(1000);
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

    // this.sericeProvider.getEPRuntime().sendEvent(
    // new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
    EPRuntime epRuntime = this.sericeProvider.getEPRuntime();
    epRuntime.sendEvent(new CurrentTimeEvent(0));
    // EPServiceProviderIsolated isolatedService =
    // sericeProvider.getEPServiceIsolated("suspendedStmts");
    // EPRuntimeIsolated runtimIsolated = isolatedService.getEPRuntime();
    // runtimIsolated.sendEvent(new TimerControlEvent(ClockType.CLOCK_EXTERNAL));
    // runtimIsolated.sendEvent(new CurrentTimeEvent(0));
    // --------------------------------------------
    // ----------- Creaet EPL --------------------
    // --------------------------------------------
    EPAdministrator admin = sericeProvider.getEPAdministrator();
    for (int i = 0, size = listEventQuery.size(); i < size; i++) {
      final EventQuery eventQuery = listEventQuery.get(i);
      EventQuery newEventQuery = EventQueryProcessor.processEventQuery(eventQuery);

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

        String tempTable = StringUtils.md5Java(eventQuery.getRuleName());
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
        // epl =
        // "insert into " + smallBucketWindow + " select " + StringUtils.convertField(select)
        // + ", hashKey as hashKey, time as time from " + from + ".win:time_batch("
        // + smallBucket + ") " + where + " " + group + " " + having;// + " output last every " +
        // // smallBucket;
        epl = epl.replaceAll(" +", " ");
        EPStatement statementInsert = admin.createEPL(epl, eventQuery.getRuleName());
        listStatement.add(statementInsert);
        // create EPL for big bucket and add listener for statement
        epl =
            " " + "select " + StringUtils.convertField2(select)
                + ", hashKey as hashKey, time as time FROM " + smallBucketWindow + ".win:time("
                + bigBucket + ") " + group;
        epl = epl.replaceAll(" +", " ");
        EPStatement statement = admin.createEPL(epl, eventQuery.getRuleName());
        listStatement.add(statement);
        // Add listener, default listener is enable = false
        statement.addListener(new EsperListener(eventQuery));



        // EPL for window every timeframe
      } else if ((bigBucket == null || Constants.EMPTY_STRING.equals(bigBucket))
          && (smallBucket != null && !Constants.EMPTY_STRING.equals(smallBucket))) {
        String tempTable = StringUtils.md5Java(eventQuery.getRuleName());
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
                  + smallBucket + ")] terminated after " + smallBucket;
        }
        epl = epl.replaceAll(" +", " ");
        admin.createEPL(epl);
        // create smallbucket aggregation
        epl =
            String
                .format(
                    "context "
                        + context
                        + " SELECT %s, hashKey as hashKey, time as time FROM %s %s %s %s output last every 1 second",
                    StringUtils.convertField(select), from + ".win:time(" + smallBucket + ")",
                    where, group, having);
        epl = epl.replaceAll(" +", " ");
        EPStatement statement = admin.createEPL(epl, eventQuery.getRuleName());
        statement.addListener(new EsperListener(eventQuery));
        listStatement.add(statement);
      } else {
        // TODO nothing to do to create EPL for this rule, this rule is invalid
      }
    }

    // --------------------------------------
    // Control timer start timer for esper
    // --------------------------------------
    if (backFill == true) {
      if (startTime == -1) {
        logger.error("No define how far to backfill");
      } else {
        // this.sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        // ---------------------------------------
        // Backfill with history event
        // ---------------------------------------
        // if this init is backfill mode
        // EPStatement[] array = new EPStatement[listStatement.size()];
        // listStatement.toArray(array);
        // EPServiceProviderIsolated isolatedService =
        // sericeProvider.getEPServiceIsolated("suspendedStmts");
        // EPRuntimeIsolated runtimIsolated = isolatedService.getEPRuntime();
        // runtimIsolated.sendEvent(new CurrentTimeEvent(0));
        // isolatedService.getEPAdministrator().addStatement(array);
        long lastEventTime = this.feedHistoricalEvent(startTime, null, epRuntime);
        // isolatedService.getEPAdministrator().removeStatement(array);
        // isolatedService.destroy();

        // Set current time for esper
        // this.sericeProvider.getEPRuntime().sendEvent(
        // new TimerControlEvent(ClockType.CLOCK_EXTERNAL));
        // startTime = System.currentTimeMillis();
        epRuntime.sendEvent(new CurrentTimeEvent(lastEventTime + 1000));
        epRuntime.sendEvent(new TimerControlEvent(ClockType.CLOCK_INTERNAL));
      }
    } else {
      // TimerControlEvent(ClockType.CLOCK_EXTERNAL));
      // startTime = 0;// System.currentTimeMillis();
      // this.sericeProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime));
      // this.sericeProvider.getEPRuntime().sendEvent(new
      // TimerControlEvent(ClockType.CLOCK_INTERNAL));
    }


    // ---------------------------------------
    // start listener enable = true
    // ---------------------------------------
    this.startListener();
  }

  /**
   * Start all statement
   */
  public void startAllStatement() {
    for (int i = 0, size = listStatement.size(); i < size; i++) {
      listStatement.get(i).start();
    }
  }

  /**
   * Stop all statement
   */
  public void stopAllStatement() {
    for (int i = 0, size = listStatement.size(); i < size; i++) {
      listStatement.get(i).stop();
    }
  }

  /**
   * Stop statement
   * 
   * @param statementName
   */
  public void stopStatement(String statementName) {
    sericeProvider.getEPAdministrator().getStatement(statementName).stop();
  }

  /**
   * Start statement
   * 
   * @param statementName
   */
  public void startStatement(String statementName) {
    sericeProvider.getEPAdministrator().getStatement(statementName).start();
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
  public long feedHistoricalEvent(long startTime, EPRuntimeIsolated runtimIsolated,
      EPRuntime epRunTime) throws Exception {
    // get historical event from DB
    List<Event> listEvent = CassandraRepository.getInstance().getEvent(startTime);
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
      System.out.println("test:" + new Date());
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

package com.lunex.eventprocessor.webservice.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.google.common.base.Strings;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventQuery.EventQueryType;
import com.lunex.eventprocessor.core.EventQueryException;
import com.lunex.eventprocessor.core.EventQuery.EventQueryStatus;
import com.lunex.eventprocessor.core.EventQueryException.ExptionAction;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.core.utils.TimeUtil;
import com.lunex.eventprocessor.webservice.netty.CallbackHTTPVisitor;
import com.lunex.eventprocessor.webservice.netty.NettyHttpSnoopClient;
import com.lunex.eventprocessor.webservice.rest.EventProcessorWebServiceFactory;

public class EventProcessorServiceAdmin {

  private CassandraRepository cassandraRepository;
  private EventProcessorWebServiceFactory factory;

  public EventProcessorServiceAdmin(CassandraRepository cassandraRepository,
      EventProcessorWebServiceFactory factory) {
    this.cassandraRepository = cassandraRepository;
    this.factory = factory;
  }

  public void addRuleException(String eventName, String ruleName, String action, String datetinme,
      String filter) throws Exception {
    Map<String, Object> map = new HashMap<String, Object>();
    if (!Strings.isNullOrEmpty(filter)) {
      map = JsonHelper.toMap(new JSONObject(filter));
    }
    EventQueryException eventQueyException =
        new EventQueryException(eventName, ruleName, ExptionAction.getContentType(action),
            TimeUtil.convertStringToDate(datetinme, "dd/MM/yyyy HH:mm:ss"), map);
    cassandraRepository.insertEventQueryException(eventQueyException);
  }

  /**
   * Add rule to db
   * 
   * @param eventName
   * @param ruleName
   * @param data
   * @param fields
   * @param filters
   * @param aggregateField
   * @param having
   * @param smallBucket
   * @param bigBucket
   * @param conditions
   * @param description
   * @throws Exception
   */
  public void addRule(String eventName, String ruleName, String data, String fields,
      String filters, String aggregateField, String having, Integer type, Integer weight,
      String smallBucket, String bigBucket, String conditions, String description,
      EventQueryStatus status) throws Exception {
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
    switch (type) {
      case 0:
        eventQuery.setType(EventQueryType.DEFAULT);
        break;
      case 1:
        eventQuery.setType(EventQueryType.DAY_OF_WEEK);
        break;
      default:
        eventQuery.setType(EventQueryType.DEFAULT);
        break;
    }
    eventQuery.setWeight(weight);
    if (status == null) {
      status = EventQueryStatus.STOP;
    }
    eventQuery.setStatus(status);
    cassandraRepository.insertEventQuery(eventQuery);
  }

  public void updateRule(String eventName, String ruleName, String data, String fields,
      String filters, String aggregateField, String having, Integer type, Integer weight,
      String smallBucket, String bigBucket, String conditions, String description, Boolean autoStart)
      throws Exception {
    List<EventQuery> rules = cassandraRepository.getEventQueryFromDB(eventName, ruleName);
    if (rules == null || rules.isEmpty()) {
      throw new Exception("Not exist rule");
    }
    EventQueryStatus status = EventQueryStatus.STOP;
    if (autoStart != null && autoStart) {
      status = EventQueryStatus.RUNNING;
    }
    // with cassandra, insert with the same key is update
    addRule(eventName, ruleName, data, fields, filters, aggregateField, having, type, weight,
        smallBucket, bigBucket, conditions, description, status);
  }

  /**
   * Delete rule from db
   * 
   * @param eventName
   * @param ruleName
   * @throws Exception
   */
  public void deleteRule(String eventName, String ruleName) throws Exception {
    EventQuery eventQuery = new EventQuery();
    eventQuery.setEventName(eventName);
    eventQuery.setRuleName(ruleName);
    cassandraRepository.deleteEventQuery(eventQuery);
  }

  /**
   * Call event handler service to start rule
   * 
   * @param eventName
   * @param ruleName
   * @param backfill
   * @param backfillTime
   * @return
   * @throws Exception
   */
  public Map<String, Object> startRule(String eventName, String ruleName, Boolean backfill,
      String backfillTime) throws Exception {
    Map<String, Object> result = new HashMap<String, Object>();
    for (int i = 0; i < factory.getHandlerServiceUrl().length; i++) {
      final CountDownLatch countdown = new CountDownLatch(1);
      CallbackHTTPVisitor callback = new CallbackHTTPVisitor() {
        @Override
        public void doJob(ChannelHandlerContext ctx, Object msg) {
          if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            if (httpContent.content() != null && httpContent.content().isReadable()) {
              ByteBuf bytes = httpContent.content();
              String httpResponseContent = bytes.toString(CharsetUtil.UTF_8);
              setResponseContent(httpResponseContent);
            }
          }
          if (msg instanceof LastHttpContent) {
            countdown.countDown();
          }
        }
      };

      NettyHttpSnoopClient client =
          new NettyHttpSnoopClient(factory.getHandlerServiceUrl()[i] + "/start/rule?evtName="
              + eventName + "&ruleName=" + ruleName + "&backfill=" + backfill + "&backfillTime="
              + backfillTime, callback);
      client.postRequest(Constants.EMPTY_STRING, HttpMethod.POST);
      // Wait to get hashKey
      countdown.await(10000, TimeUnit.MILLISECONDS);
      result.put(factory.getHandlerServiceUrl()[i], callback.getResponseContent());
    }
    return result;
  }

  /**
   * Call Event handler service to stop rule
   * 
   * @param eventName
   * @param ruleName
   * @return
   * @throws Exception
   */
  public Map<String, Object> stopRule(String eventName, String ruleName) throws Exception {
    Map<String, Object> result = new HashMap<String, Object>();
    for (int i = 0; i < factory.getHandlerServiceUrl().length; i++) {
      final CountDownLatch countdown = new CountDownLatch(1);
      CallbackHTTPVisitor callback = new CallbackHTTPVisitor() {
        @Override
        public void doJob(ChannelHandlerContext ctx, Object msg) {
          if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            if (httpContent.content() != null && httpContent.content().isReadable()) {
              ByteBuf bytes = httpContent.content();
              String httpResponseContent = bytes.toString(CharsetUtil.UTF_8);
              setResponseContent(httpResponseContent);
            }
          }
          if (msg instanceof LastHttpContent) {
            countdown.countDown();
          }
        }
      };
      NettyHttpSnoopClient client =
          new NettyHttpSnoopClient(factory.getHandlerServiceUrl()[i] + "/stop/rule?evtName="
              + eventName + "&ruleName=" + ruleName, callback);
      client.postRequest(Constants.EMPTY_STRING, HttpMethod.POST);
      // Wait to get hashKey
      countdown.await(10000, TimeUnit.MILLISECONDS);
      result.put(factory.getHandlerServiceUrl()[i], callback.getResponseContent());
    }
    return result;
  }

  public Map<String, Object> changeRule(String eventName, String ruleName, Boolean backfill,
      String backfillTime, Boolean autoStart) throws Exception {
    Map<String, Object> result = new HashMap<String, Object>();
    for (int i = 0; i < factory.getHandlerServiceUrl().length; i++) {
      final CountDownLatch countdown = new CountDownLatch(1);
      CallbackHTTPVisitor callback = new CallbackHTTPVisitor() {
        @Override
        public void doJob(ChannelHandlerContext ctx, Object msg) {
          if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            if (httpContent.content() != null && httpContent.content().isReadable()) {
              ByteBuf bytes = httpContent.content();
              String httpResponseContent = bytes.toString(CharsetUtil.UTF_8);
              setResponseContent(httpResponseContent);
            }
          }
          if (msg instanceof LastHttpContent) {
            countdown.countDown();
          }
        }
      };
      if (backfill == null) {
        backfill = false;
      }
      NettyHttpSnoopClient client =
          new NettyHttpSnoopClient(factory.getHandlerServiceUrl()[i] + "/update/rule?evtName="
              + eventName + "&ruleName=" + ruleName + "&backfill=" + backfill + "&backfillTime="
              + backfillTime + "&autoStart=" + autoStart, callback);
      client.postRequest(Constants.EMPTY_STRING, HttpMethod.POST);
      // Wait to get hashKey
      countdown.await(10000, TimeUnit.MILLISECONDS);
      result.put(factory.getHandlerServiceUrl()[i], callback.getResponseContent());
    }
    return result;
  }
}

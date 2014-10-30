package com.lunex.eventprocessor.webservice.service;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

import com.google.gson.Gson;
import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.webservice.netty.CallbackHTTPVisitor;
import com.lunex.eventprocessor.webservice.netty.NettyHttpSnoopClient;
import com.lunex.eventprocessor.webservice.rest.EventProcessorWebServiceFactory;

public class EventProcessorService {

  final static org.slf4j.Logger logger = LoggerFactory.getLogger(EventProcessorService.class);

  private CassandraRepository cassandraRepository;
  private EventProcessorWebServiceFactory factory;

  public EventProcessorService(CassandraRepository cassandraRepository,
      EventProcessorWebServiceFactory factory) {
    this.cassandraRepository = cassandraRepository;
    this.factory = factory;
  }

  /**
   * Add new event to event processor
   * 
   * @param event
   * @param seq
   * @return
   * @throws Exception
   */
  public String addEvent(Event event, long seq) throws Exception {
    // Create Netty http client
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
    NettyHttpSnoopClient httpClient =
        new NettyHttpSnoopClient(factory.getInputProcessorUrl() + "?evtName=" + event.getEvtName()
            + "&seq=" + seq, callback);

    try {
      // Call event input processor
      httpClient.postEvent(event);
      // Wait to get hashKey
      countdown.await(10000, TimeUnit.MILLISECONDS);
      String httpResponseContent = callback.getResponseContent();
      if (StringUtils.isJSONValid(httpResponseContent)) {
        JSONObject json = new JSONObject(httpResponseContent);
        if (json.getBoolean("result"))
          return json.getString("hashKey");
      }
    } catch (Exception e1) {
      throw e1;
    }
    throw new Exception("Can not get any hashKey");
  }

  /**
   * Check result for event
   * 
   * @param hashKey
   * @return
   * @throws Exception
   */
  public String checkEvent(String eventName, String hashKey) throws Exception {
    // TODO
    List<EventResult> eventResults = cassandraRepository.getEventResult(eventName, hashKey);
    if (eventResults != null && !eventResults.isEmpty()) {
      EventResult eventResult = eventResults.get(0);
      Gson gson = new Gson();
      return gson.toJson(eventResult).toString();
    }
    return null;
  }
}

package com.lunex.eventprocessor.handler.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.QueryHierarchy;
import com.lunex.eventprocessor.handler.output.DataAccessOutputHandler;

public class KairosDBProcessor implements Processor {

  static final Logger logger = LoggerFactory.getLogger(KairosDBProcessor.class);

  private QueryHierarchy queryHierarchy;

  public void consume(Event event) {
    // ******************************************//
    // Consume event by send event to KairosDB *//
    // ******************************************//
    DataAccessOutputHandler.sendRawEventToKairosDB(event, queryHierarchy);

  }

  public QueryHierarchy getHierarchy() {
    return this.queryHierarchy;
  }

  public void setHierarchy(QueryHierarchy hierarchy) {
    this.queryHierarchy = hierarchy;
  }

}

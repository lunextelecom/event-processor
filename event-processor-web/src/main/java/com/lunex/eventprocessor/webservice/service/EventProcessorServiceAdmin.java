package com.lunex.eventprocessor.webservice.service;

import com.lunex.eventprocessor.core.EventQueryException;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;

public class EventProcessorServiceAdmin {

  private CassandraRepository cassandraRepository;

  public EventProcessorServiceAdmin(CassandraRepository cassandraRepository) {
    this.cassandraRepository = cassandraRepository;
  }
  
  public void addRuleException(EventQueryException eventQueyException) throws Exception {
    cassandraRepository.insertEventQueryException(eventQueyException);
  }
}

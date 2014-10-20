package com.lunex.eventprocessor.webservice.service;

import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;

public class EventProcessorService {

  private CassandraRepository cassandraRepository;

  public EventProcessorService(CassandraRepository cassandraRepository) {
    this.cassandraRepository = cassandraRepository;
  }

  public String addEvent(Event event) {
    String hashKey = "";
    // TODO
    return hashKey;
  }
  
  public String checkEvent(Event event) {
    // TODO
    return null;
  }
  
  public String checkEvent(String hashKey) {
    // TODO
    return hashKey;
  }
  
  public String addAndCheck(Event event) {
    // TODO
    return null;
  }
}

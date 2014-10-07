//package com.lunex.eventprocessor.handler.continuousquery;
//
//import com.lunex.eventprocessor.handler.bean.Tick;
//
///**
// * Process event handler
// *
// */
//public class EventProcessorHandler {
//
//  private EventProcessorServiceProvider eventProcessorService;
//
//  public EventProcessorServiceProvider getEventProcessorService() {
//    return this.eventProcessorService;
//  }
//
//  public EventProcessorHandler() {}
//
//  /**
//   * Read rule from DB and load into esper
//   */
//  public void loadEPLRuntime(String providerURI) {
//    // TODO
//    eventProcessorService = new EventProcessorServiceProvider(providerURI, this.loadConfig());
//    this.loadRuleFromDB();
//    this.createEPLFromRule();
//  }
//
//  private void loadRuleFromDB() {
//    // TODO
//  }
//
//  private void createEPLFromRule() {
//    // TODO
//  }
//
//  private EventProcessorConfiguration loadConfig() {
//    // TODO
//    EventProcessorConfiguration configuration = new EventProcessorConfiguration();
//    configuration.addEventType("Tick", Tick.class);
//    return configuration;
//  }
//
//  private EventProcessorStatement createEPL(String query) {
//    return this.eventProcessorService.createEPL(query);
//  }
//
//  public void sendEvent(byte[] object) {
//    // TODO convert byte to Class event
//    // send object to EPL
//    this.eventProcessorService.getEPRuntime().sendEvent(object);
//  }
//}

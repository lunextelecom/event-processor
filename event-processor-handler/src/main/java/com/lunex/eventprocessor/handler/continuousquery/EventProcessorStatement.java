//package com.lunex.eventprocessor.handler.continuousquery;
//
//import com.espertech.esper.client.EPStatement;
//
///**
// * Class statement add and destroy listener for EPL
// *
// */
//public class EventProcessorStatement {
//  private EPStatement statement;
//
//  public EPStatement getStatement() {
//    return statement;
//  }
//
//  public void setStatement(EPStatement statement) {
//    this.statement = statement;
//  }
//
//  public EventProcessorStatement() {
//
//  }
//
//  public EventProcessorStatement(EPStatement statement) {
//    this.statement = statement;
//  }
//
//  public void addListener(EventUpdateListener listener) {
//    this.statement.addListener(listener);
//  }
//
//  public void destroy() {
//    this.statement.destroy();
//  }
//}

package com.lunex.eventprocessor.core;

/**
 * This is just a class used to represent the content of the Rules that is read from datastore. It
 * is not the actually esper query. It’s the pieces that we used to build the esper query
 */
public class EventQuery {

  private String data;
  private String fields;
  private String filters;
  private String aggregateField;

  QueryFuture getFuture() {
    return null;
  }
}

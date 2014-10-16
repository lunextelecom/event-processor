package com.lunex.eventprocessor.handler.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class WebConfiguration extends Configuration {
  private EventHandlerApiServiceFactory ccServiceFactory = new EventHandlerApiServiceFactory();

  @JsonProperty("app")
  public EventHandlerApiServiceFactory getCcServiceFactory() {
    return this.ccServiceFactory;
  }

  @JsonProperty("app")
  public void setMessageQueueFactory(EventHandlerApiServiceFactory ccServiceFactory) {
    this.ccServiceFactory = ccServiceFactory;
  }
}

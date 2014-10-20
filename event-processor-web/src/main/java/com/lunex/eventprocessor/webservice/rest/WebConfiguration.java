package com.lunex.eventprocessor.webservice.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class WebConfiguration extends Configuration {
  private EventProcessorWebServiceFactory ccServiceFactory = new EventProcessorWebServiceFactory();

  @JsonProperty("app")
  public EventProcessorWebServiceFactory getCcServiceFactory() {
    return this.ccServiceFactory;
  }

  @JsonProperty("app")
  public void setMessageQueueFactory(EventProcessorWebServiceFactory ccServiceFactory) {
    this.ccServiceFactory = ccServiceFactory;
  }
}

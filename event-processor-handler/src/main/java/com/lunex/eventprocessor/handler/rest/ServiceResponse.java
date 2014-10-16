package com.lunex.eventprocessor.handler.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceResponse {

  private String message;
  private boolean result;

  public ServiceResponse(String message, boolean result) {
    this.message = message;
    this.result = result;
  }

  @JsonProperty
  public String getMessage() {
    return message;
  }

  @JsonProperty
  public boolean getResult() {
    return result;
  }
}

package com.lunex.eventprocessor.handler.rest;

import com.fasterxml.jackson.annotation.JsonProperty;


public class EventHandlerApiServiceFactory {
  private String dbName;
  private String dbHost;
  private String dbUsername;
  private String dbPassword;
  private String cacheConfig;
  private String statsdHost;
  private String statsdAppPrefix;

  @JsonProperty
  public String getDbName() {
    return this.dbName;
  }

  @JsonProperty
  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  @JsonProperty
  public String getDbHost() {
    return this.dbHost;
  }

  @JsonProperty
  public void setDbHost(String dbHost) {
    this.dbHost = dbHost;
  }

  @JsonProperty
  public String getDbUsername() {
    return this.dbUsername;
  }

  @JsonProperty
  public void setDbUsername(String dbUsername) {
    this.dbUsername = dbUsername;
  }

  @JsonProperty
  public String getDbPassword() {
    return this.dbPassword;
  }

  @JsonProperty
  public void setDbPassword(String dbPassword) {
    this.dbPassword = dbPassword;
  }

  @JsonProperty
  public String getCacheConfig() {
    return this.cacheConfig;
  }

  @JsonProperty
  public void setCacheConfig(String cacheConfig) {
    this.cacheConfig = cacheConfig;
  }

  @JsonProperty
  public String getStatsdHost() {
    return this.statsdHost;
  }

  @JsonProperty
  public void setStatsdHost(String statsdHost) {
    this.statsdHost = statsdHost;
  }

  @JsonProperty
  public String getStatsdAppPrefix() {
    return this.statsdAppPrefix;
  }

  @JsonProperty
  public void setStatsdAppPrefix(String statsdAppPrefix) {
    this.statsdAppPrefix = statsdAppPrefix;
  }  
}

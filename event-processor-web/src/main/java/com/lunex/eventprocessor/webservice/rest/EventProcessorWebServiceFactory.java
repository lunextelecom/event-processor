package com.lunex.eventprocessor.webservice.rest;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.webservice.service.EventProcessorService;
import com.lunex.eventprocessor.webservice.service.EventProcessorServiceAdmin;

/**
 * Factory for web service
 * 
 *
 */
public class EventProcessorWebServiceFactory {
  private String dbName;
  private String dbHost;
  private String dbUsername;
  private String dbPassword;
  private String cacheConfig;
  private String statsdHost;
  private String statsdAppPrefix;
  private String inputProcessorUrl;
  private String[] handlerServiceUrl;

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

  @JsonProperty
  public void setInputProcessorUrl(String inputProcessorUrl) {
    this.inputProcessorUrl = inputProcessorUrl;
  }

  @JsonProperty
  public String getInputProcessorUrl() {
    return this.inputProcessorUrl;
  }

  @JsonProperty
  public String[] getHandlerServiceUrl() {
    return handlerServiceUrl;
  }

  @JsonProperty
  public void setHandlerServiceUrl(String[] handlerServiceUrl) {
    this.handlerServiceUrl = handlerServiceUrl;
  }

  /**
   * Create Event Processor service
   * 
   * @param environment
   * @return
   * @throws Exception
   */
  public EventProcessorService buildEventProcessorService(Environment environment) throws Exception {
    CassandraRepository cassandra = CassandraRepository.getInstance(getDbHost(), getDbName());
    EventProcessorService service = new EventProcessorService(cassandra, this);
    environment.lifecycle().manage(new Managed() {
      // @Override
      public void stop() throws Exception {}

      // @Override
      public void start() throws Exception {}
    });
    return service;
  }

  /**
   * Create Event processor admin service
   * 
   * @param environment
   * @return
   * @throws Exception
   */
  public EventProcessorServiceAdmin buildEventProcessorServiceAdmin(Environment environment)
      throws Exception {
    CassandraRepository cassandra = CassandraRepository.getInstance(getDbHost(), getDbName());
    EventProcessorServiceAdmin service = new EventProcessorServiceAdmin(cassandra, this);
    environment.lifecycle().manage(new Managed() {
      // @Override
      public void stop() throws Exception {}

      // @Override
      public void start() throws Exception {}
    });
    return service;
  }
}

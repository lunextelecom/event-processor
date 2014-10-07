package com.lunex.eventprocessor.handler.continuousquery;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;

/**
 * Provide service for process EPL 
 *
 */
public class EventProcessorServiceProvider {

  private EPServiceProvider serviceProvider;
  private EPAdministrator admin;

  public EventProcessorServiceProvider(String providerURI, EventProcessorConfiguration configuration) {
    serviceProvider = EPServiceProviderManager.getProvider(providerURI, configuration.getConfig());
    admin = serviceProvider.getEPAdministrator();
  }

  public EventProcessorStatement createEPL(String query) {
    return new EventProcessorStatement(admin.createEPL(query));
  }

  public EventProcessorRuntime getEPRuntime() {
    return new EventProcessorRuntime(serviceProvider.getEPRuntime());
  }

  public void destroyAllStatement() {
    this.admin.destroyAllStatements();
  }
}

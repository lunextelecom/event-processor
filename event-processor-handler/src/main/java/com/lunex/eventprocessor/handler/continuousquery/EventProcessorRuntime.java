package com.lunex.eventprocessor.handler.continuousquery;

import com.espertech.esper.client.EPRuntime;

/**
 * Class run time processor for send event to EPL
 * @author Administrator
 *
 */
public class EventProcessorRuntime {

  private EPRuntime epRuntime;
  
  public EventProcessorRuntime(EPRuntime epRuntime) {
    this.epRuntime = epRuntime;
  }
  
  public void sendEvent(Object object) {
    this.epRuntime.sendEvent(object);
  }
}

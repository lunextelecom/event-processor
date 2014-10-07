package com.lunex.eventprocessor.handler.processor;

import com.lunex.eventprocessor.core.Event;

public interface EventConsumer {
  
  /**
   * Consume event
   * @param event
   */
  void consume(Event event);
}

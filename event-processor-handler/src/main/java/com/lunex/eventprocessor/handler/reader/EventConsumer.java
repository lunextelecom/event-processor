package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.Event;

public interface EventConsumer {
  
  /**
   * Consume event
   * @param event
   */
  void consume(Event event);
}

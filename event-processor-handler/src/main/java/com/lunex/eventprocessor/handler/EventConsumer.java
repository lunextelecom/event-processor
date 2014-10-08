package com.lunex.eventprocessor.handler;

import com.lunex.eventprocessor.Event;

/**
 * Consumer of event
 */
public interface EventConsumer {
  void consume(Event event);
}

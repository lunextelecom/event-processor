package com.lunex.eventprocessor.handler;

import com.lunex.eventprocessor.Event;

/**
 * Created by jerryj on 10/2/14.
 */
public interface EventConsumer {
  void consume(Event event);
}

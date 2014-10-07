package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.bean.Event;

public interface IEventConsumer {
  void consume(Event event);
}

package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.bean.Event;
import com.lunex.eventprocessor.handler.kafka.KafkaSimpleConsumer;
import com.lunex.eventprocessor.handler.utils.Configuration;

public class KafkaReader implements IEventReader {

  public Event readNext() {
    // TODO: no implement this function for KafkaReader
    return null;
  }

  public void read(IEventConsumer consumer) {
    // TODO Auto-generated method stub

  }

  public void stop() {
    // TODO Auto-generated method stub

  }

}

package com.lunex.eventprocessor.handler;

import com.lunex.eventprocessor.Event;

/**
 * Created by jerryj on 9/26/14.
 */
public class KafkaReader implements EventReader {



  @Override
  public Event readNext() {
    return null;
  }

  @Override
  public void read(EventConsumer consumer) {
  }

  @Override
  public void stop() {

  }
}

package com.lunex.eventprocessor.handler;

import com.lunex.eventprocessor.Event;
import com.lunex.eventprocessor.handler.EventConsumer;

/**
 * Created by jerryj on 10/2/14.
 */
public interface EventReader {

  /**
   * Read a single message, may not be supported by all reader
   * @return a single Event if it exist, otherwise null
   */
  public Event readNext();

  /**
   * Read data and pass into consumer.  This method will block, caller of this method should start the thread.
   * @param consumer
   */
  public void read(EventConsumer consumer);

  /**
   * used to stop the read
   */
  public void stop();
}

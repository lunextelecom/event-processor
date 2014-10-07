package com.lunex.eventprocessor.handler.reader;

import com.lunex.eventprocessor.core.bean.Event;

public interface IEventReader {
  /**
   * Read a single message, may not be supported by all reader
   * 
   * @return a single Event if it exist, otherwise null
   */
  public Event readNext();

  /**
   * Read data and pass into consumer. This method will block, caller of this method should start
   * the thread.
   * 
   * @param consumer
   */
  public void read(IEventConsumer consumer);

  /**
   * used to stop the read
   */
  public void stop();
}

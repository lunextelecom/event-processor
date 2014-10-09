package com.lunex.eventprocessor.handler.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;

public class DataAccessOutput {
  static final Logger logger = LoggerFactory.getLogger(DataAccessOutput.class);

  public static void insertRawEvent(Event event) {
    // insert raw event to db
    final Event insertEvent = event;
    Thread insertEventThread = new Thread(new Runnable() {
      public void run() {
        try {
          CassandraRepository.getInstance().insertEventToDB(insertEvent);
        } catch (Exception e) {
          logger.error(e.getMessage());
        }
      }
    });
    insertEventThread.start();
  }

}

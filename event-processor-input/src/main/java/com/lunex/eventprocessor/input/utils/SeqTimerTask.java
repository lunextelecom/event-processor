package com.lunex.eventprocessor.input.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.input.App;
import com.lunex.eventprocessor.input.Seq;

/**
 * Class handle seq of event
 *
 */
public class SeqTimerTask extends TimerTask {

  static final Logger logger = LoggerFactory.getLogger(App.class);

  private Queue<Seq> queueSeq;
  private Timer timer;

  public SeqTimerTask() {
    queueSeq = new LinkedList<Seq>();
    timer = new Timer(true);
  }

  public void addSeq(Seq seq) {
    queueSeq.add(seq);
    // logger.info(queueSeq.toString());
  }

  /**
   * Start time task
   */
  public void start(int interval) {
    timer.scheduleAtFixedRate(this, 0, interval * 1000);
  }

  /**
   * Stop time task
   */
  public void stop() {
    timer.cancel();
  }

  /**
   * Check event with seq is existed in queue
   * 
   * @param seq
   * @return
   */
  public boolean contains(Seq seq) {
    if (seq == null) {
      return true;
    }
    for (int i = 0; i < queueSeq.size(); i++) {
      Seq seqTemp = queueSeq.peek();
      if (seqTemp == null) {
        return false;
      }
      if (seq.getEventName().equals(seqTemp.getEventName()) && seq.getSeq() == seqTemp.getSeq()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void run() {
    Seq seq = null;
    if (queueSeq.size() == 0) {
      return;
    }
    while (true) {
      seq = queueSeq.peek();
      if (seq == null) {
        break;
      }
      if (seq.getTime() < (System.currentTimeMillis() - (1000 * Configuration.timeStoreSeq))) {
        queueSeq.poll();
      } else {
        break;
      }
    }
  }

}

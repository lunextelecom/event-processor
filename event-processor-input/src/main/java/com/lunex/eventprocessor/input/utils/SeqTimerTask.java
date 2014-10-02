package com.lunex.eventprocessor.input.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.input.App;
import com.lunex.eventprocessor.input.beans.Seq;

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
//    logger.info(queueSeq.toString());
  }

  public void start() {
    timer.scheduleAtFixedRate(this, 0, 1 * 1000);
  }

  public void stop() {
    timer.cancel();
  }

  public boolean contains(Seq seq) {
    if (seq == null) {
      return true;
    }
    for (int i = 0; i < queueSeq.size(); i++) {
      Seq seqTemp = queueSeq.peek();
      if (seqTemp == null) {
        return false;
      }
      if (seq.getEventName().equals(seqTemp.getEventName())
          && seq.getSeq().equals(seqTemp.getSeq())) {
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

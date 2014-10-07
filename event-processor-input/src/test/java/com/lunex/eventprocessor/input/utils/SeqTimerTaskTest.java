package com.lunex.eventprocessor.input.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import com.lunex.eventprocessor.input.Seq;

public class SeqTimerTaskTest {

  @Test
  public void testContains() {
    SeqTimerTask a = new SeqTimerTask();
    a.start(2);
    Seq seq = new Seq(1L, "test", System.currentTimeMillis());
    a.addSeq(seq);
    assertEquals(true, a.contains(seq));    
    try {
      Thread.sleep(15000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertEquals(false, a.contains(seq));
    a.stop();
  }

}

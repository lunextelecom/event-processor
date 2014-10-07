package com.lunex.eventprocessor.input;

public class Seq {

  private long seq;
  private String eventName;
  private long time; // by mili sec

  public long getSeq() {
    return seq;
  }

  public void setSeq(long seq) {
    this.seq = seq;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public Seq(long seq, String eventName, long time) {
    this.seq = seq;
    this.eventName = eventName;
    this.time = time;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

}

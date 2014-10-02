package com.lunex.eventprocessor.input.beans;

public class Seq {

  private Long seq;
  private String eventName;
  private Long time; // by mili sec

  public Long getSeq() {
    return seq;
  }

  public void setSeq(Long seq) {
    this.seq = seq;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public Seq(Long seq, String eventName, Long time) {
    this.seq = seq;
    this.eventName = eventName;
    this.time = time;
  }

  public Long getTime() {
    return time;
  }

  public void setTime(Long time) {
    this.time = time;
  }

}

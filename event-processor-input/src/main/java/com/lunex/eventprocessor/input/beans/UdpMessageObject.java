package com.lunex.eventprocessor.input.beans;

public class UdpMessageObject {

  private String evtName;
  private Long seq;
  private String contentType;
  private Long contentLength = 0L;
  private String payLoad;
  private byte[] payLoadBytes;
  private String hashKey;

  public String getEvtName() {
    return evtName;
  }

  public void setEvtName(String evtName) {
    this.evtName = evtName;
  }

  public Long getSeq() {
    return seq;
  }

  public void setSeq(Long seq) {
    this.seq = seq;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Long getContentLength() {
    return contentLength;
  }

  public void setContentLength(Long contentLength) {
    this.contentLength = contentLength;
  }

  public String getPayLoad() {
    return payLoad;
  }

  public void setPayLoad(String payLoad) {
    this.payLoad = payLoad;
  }

  public byte[] getPayLoadBytes() {
    return payLoadBytes;
  }

  public void setPayLoadBytes(byte[] payLoadBytes) {
    this.payLoadBytes = payLoadBytes;
  }

  public String getHashKey() {
    return hashKey;
  }

  public void setHashKey(String hashKey) {
    this.hashKey = hashKey;
  }

}

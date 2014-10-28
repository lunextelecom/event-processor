package com.lunex.eventprocessor.webservice.netty;

import io.netty.channel.ChannelHandlerContext;

public class CallbackHTTPVisitor {

  private String responseContent;

  public void doJob(ChannelHandlerContext ctx, Object msg) {};

  public String getResponseContent() {
    return this.responseContent;
  }

  public void setResponseContent(String content) {
    this.responseContent = content;
  }
}

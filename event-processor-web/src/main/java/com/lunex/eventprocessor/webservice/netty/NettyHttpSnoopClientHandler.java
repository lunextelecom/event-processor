package com.lunex.eventprocessor.webservice.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;

/**
 * Netty http client handler
 * 
 * @author My PC
 *
 */
public class NettyHttpSnoopClientHandler extends SimpleChannelInboundHandler<HttpObject> {

  static final Logger logger = LoggerFactory.getLogger(NettyHttpSnoopClientHandler.class);

  private CallbackHTTPVisitor callback;

  public NettyHttpSnoopClientHandler(CallbackHTTPVisitor callback) {
    this.callback = callback;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    if (callback != null) {
      callback.doJob(ctx, msg);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    // cause.printStackTrace();
    ctx.close();
  }
}

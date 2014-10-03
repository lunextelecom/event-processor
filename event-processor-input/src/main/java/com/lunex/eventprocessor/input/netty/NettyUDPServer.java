package com.lunex.eventprocessor.input.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.input.utils.Configuration;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Netty UDP server 
 *
 */
public class NettyUDPServer {

  Logger logger = LoggerFactory.getLogger(NettyUDPServer.class);

  private Bootstrap bootStrap;
  private int port;
  private Channel channel;
  private EventLoopGroup group;

  public NettyUDPServer(int port) {
    this.port = port;
  }

  /**
   * Start server UDP
   * 
   * @throws Exception
   */
  public void startServer() throws Exception {
    group = new NioEventLoopGroup(Configuration.nettyNumThread);
    try {
      bootStrap = new Bootstrap();
      bootStrap.group(group).channel(NioDatagramChannel.class)
          .option(ChannelOption.SO_BROADCAST, true).handler(new NettyUDPServerHandler());

      bootStrap.bind(this.port).sync().channel().closeFuture().await();
      logger.info("Start udp server");
    } catch (Exception e) {
      logger.error(e.getMessage());
    } finally {
      group.shutdownGracefully();
    }
  }

  /**
   * Stop server UDP
   */
  public void stopServer() {
    if (channel != null) {
      channel.close();
    }
    if (group != null) {
      group.shutdownGracefully();
    }
  }
}

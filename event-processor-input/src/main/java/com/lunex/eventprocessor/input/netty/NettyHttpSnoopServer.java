package com.lunex.eventprocessor.input.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.input.utils.Configuration;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Netty server for http protocol
 *
 */
public class NettyHttpSnoopServer {

  static final Logger logger = LoggerFactory.getLogger(NettyHttpSnoopServer.class);

  private int port;
  private ServerBootstrap bootStrap;
  private Channel channel;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public NettyHttpSnoopServer(int port) {
    this.port = port;
  }

  /**
   * Start HTTP server to listen request from client
   * 
   * @throws Exception
   */
  public synchronized void startServer() throws Exception {

    // Configure the server.

    bossGroup = new NioEventLoopGroup(Configuration.nettyNumThread);
    workerGroup = new NioEventLoopGroup(Configuration.nettyNumThread);
    try {
      bootStrap = new ServerBootstrap();
      bootStrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(new NettyHttpSnoopServerInitializer());
      channel = bootStrap.bind(port).sync().channel();

      ChannelFuture channelFuture = channel.closeFuture();
      channelFuture.sync();
      logger.info("Start http server");
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  /**
   * Shutdown server
   */
  public synchronized void stopServer() {
    channel.close();
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
  }
}

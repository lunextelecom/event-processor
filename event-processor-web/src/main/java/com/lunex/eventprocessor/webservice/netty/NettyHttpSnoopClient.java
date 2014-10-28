package com.lunex.eventprocessor.webservice.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.Event;

public class NettyHttpSnoopClient {

  static final Logger logger = LoggerFactory.getLogger(NettyHttpSnoopClient.class);

  private String url;
  private URI uri;
  private int port;
  private String host;
  private String scheme;
  public CallbackHTTPVisitor callback;
  private SslContext sslCtx;
  public Object msg;
  public Channel ch;
  public EventLoopGroup group;

  public NettyHttpSnoopClient(String url, CallbackHTTPVisitor callback) {
    this.url = url;
    this.callback = callback;
  }

  /**
   * prepare host information for processing
   * 
   * @return
   * @throws URISyntaxException
   * @throws SSLException
   */
  private boolean preProcessURL() throws URISyntaxException, SSLException {
    this.uri = new URI(url.replace(" ", "%20"));
    this.scheme = uri.getScheme();
    this.host = uri.getHost();
    this.port = uri.getPort();
    if (port == -1) {
      if ("http".equalsIgnoreCase(scheme)) {
        port = 80;
      } else if ("https".equalsIgnoreCase(scheme)) {
        port = 443;
      }
    }

    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      System.err.println("Only HTTP(S) is supported.");
      return false;
    }

    // Configure SSL context if necessary.
    final boolean ssl = "https".equalsIgnoreCase(scheme);
    if (ssl) {
      sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
    } else {
      sslCtx = null;
    }
    return true;
  }

  public boolean postEvent(Event event) throws Exception {
    try {
      if (!this.preProcessURL()) {
        return false;
      }
    } catch (URISyntaxException ex) {
      throw ex;
    } catch (SSLException ex) {
      throw ex;
    }

    // Configure the client.
    group = new NioEventLoopGroup(1);
    try {
      Bootstrap b = new Bootstrap();
      b.group(group).channel(NioSocketChannel.class)
          .handler(new NettyHttpSnoopClientInitializer(sslCtx, callback));

      // Make the connection attempt.
      ch = b.connect(host, port).sync().channel();

      // Prepare the HTTP request.
      ByteBuf content = Unpooled.copiedBuffer(event.getPayLoadStr(), CharsetUtil.UTF_8);
      HttpRequest request =
          new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri.toString(), content);
      request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
      request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, content.readableBytes());
      request.headers().set(HttpHeaders.Names.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
      request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
      request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
      request.headers().set(HttpHeaders.Names.HOST, host);

      // Send the HTTP request.
      // ChannelFuture chanel =
      ch.writeAndFlush(request);

      // Wait for the server to close the connection.
      ch.closeFuture().sync();
    } catch (Exception ex) {
      throw ex;
    }
    return true;
  }

  public boolean postRequest(String content, HttpMethod method) throws Exception {
    try {
      if (!this.preProcessURL()) {
        return false;
      }
    } catch (URISyntaxException ex) {
      throw ex;
    } catch (SSLException ex) {
      throw ex;
    }

    // Configure the client.
    group = new NioEventLoopGroup(1);
    try {
      Bootstrap b = new Bootstrap();
      b.group(group).channel(NioSocketChannel.class)
          .handler(new NettyHttpSnoopClientInitializer(sslCtx, callback));

      // Make the connection attempt.
      ch = b.connect(host, port).sync().channel();

      // Prepare the HTTP request.
      ByteBuf contentBuf = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
      HttpRequest request =
          new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri.toString(),
              contentBuf);
      request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
      request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, contentBuf.readableBytes());
      request.headers().set(HttpHeaders.Names.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
      request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
      request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
      request.headers().set(HttpHeaders.Names.HOST, host);

      // Send the HTTP request.
      // ChannelFuture chanel =
      ch.writeAndFlush(request);

      // Wait for the server to close the connection.
      ch.closeFuture().sync();
    } catch (Exception ex) {
      throw ex;
    }
    return true;
  }

  public void shutdown() {
    ch.disconnect();
    group.shutdownGracefully();
  }
}

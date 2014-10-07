package com.lunex.eventprocessor.input.netty;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.input.App;
import com.lunex.eventprocessor.input.HttpMessageObject;
import com.lunex.eventprocessor.input.Seq;
import com.lunex.eventprocessor.input.enums.EContentType;
import com.lunex.eventprocessor.input.exception.BadRequestException;
import com.lunex.eventprocessor.input.exception.InternalServerErrorException;
import com.lunex.eventprocessor.input.exception.MethodNotAllowedException;
import com.lunex.eventprocessor.input.utils.Configuration;
import com.lunex.eventprocessor.input.utils.Constant;
import com.lunex.eventprocessor.input.utils.StringUtils;

/**
 * Server handler for netty server http
 */
public class NettyHttpSnoopServerHandler extends SimpleChannelInboundHandler<HttpObject> {

  static final Logger logger = LoggerFactory.getLogger(NettyHttpSnoopServerHandler.class);

  private Boolean isException = false;
  private Exception exception;

  private HttpRequest request;
  private HttpMessageObject messageObject;

  public NettyHttpSnoopServerHandler() {

  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    // write response
    if (isException) {
      exceptionCaught(ctx, exception);
      return;
    }
    if (!writeResponse(ctx)) {
      // If keep-alive is off, close the connection once the content is fully written.
      ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    } else {
      ctx.flush();
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    if (msg instanceof HttpRequest) {
      this.request = (HttpRequest) msg;

      this.messageObject = new HttpMessageObject();
      this.messageObject.setMethod(this.request.getMethod());
      if (!this.messageObject.getMethod().equals(HttpMethod.POST)) {
        isException = true;
        exception = new MethodNotAllowedException(new Throwable("Only accept method POST"));
        return;
      }
      this.messageObject.setHeader(this.request.headers());

      QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
      Map<String, List<String>> params = queryStringDecoder.parameters();
      if (!params.isEmpty()) {
        for (Entry<String, List<String>> p : params.entrySet()) {
          String key = p.getKey();
          List<String> vals = p.getValue();
          for (String val : vals) {
            this.messageObject.getQueryParams().put(key, val);
          }
        }
      }
    }
    if (msg instanceof HttpContent) {
      if (!isException) {
        HttpContent httpContent = (HttpContent) msg;
        if (httpContent.content() != null && httpContent.content().isReadable()) {
          ByteBuf bytes = httpContent.content();
          this.messageObject.setContentLengthInByte(bytes.capacity());
          String payLoad = bytes.toString(CharsetUtil.UTF_8);
          this.messageObject.setBody(payLoad);
          this.messageObject.setBodyBytes(payLoad.getBytes(CharsetUtil.UTF_8));
          logger.info("payLoad: " + payLoad);
        }
      }
    }
    if (msg instanceof LastHttpContent) {
      if (!isException) {
        this.sendKafka();
      }
    }
  }

  /**
   * Send message to kafka
   */
  private void sendKafka() {
    // Validate
    if (this.messageObject.getQueryParams().get(Constant.EVENTNAME_PROP) == null
        || this.messageObject.getQueryParams().get(Constant.SEQ_PROP) == null) {
      isException = true;
      exception = new BadRequestException(new Throwable("Query param is not valid"));
      return;
    }

    String eventName = this.messageObject.getQueryParams().get(Constant.EVENTNAME_PROP);
    if (Constant.EMPTY_STRING.equals(eventName)) {
      isException = true;
      exception = new BadRequestException(new Throwable("Event name is empty"));
      return;
    }

    String body = this.messageObject.getBody();
    if (body == null || Constant.EMPTY_STRING.equals(body)) {
      isException = true;
      exception = new BadRequestException(new Throwable("Body is empty"));
      return;
    }

    HttpHeaders header = this.messageObject.getHeader();
    EContentType contentType = EContentType.getContentType(header.get(CONTENT_TYPE.toString()));
    if (contentType == null) {
      isException = true;
      exception = new BadRequestException(new Throwable("Wrong Content-Type"));
      return;
    } else {
      switch (contentType) {
        case JSONType:
          if (!StringUtils.isJSONValid(body)) {
            isException = true;
            exception = new BadRequestException(new Throwable("Wrong Content-Type"));
            return;
          }
          break;
        default:
          break;
      }
    }

    if (!Integer.valueOf(header.get(CONTENT_LENGTH.toString())).equals(
        this.messageObject.getContentLengthInByte())) {
      isException = true;
      exception = new BadRequestException(new Throwable("Wrong Content-Length"));
      return;
    }

    Long seq = Long.valueOf(this.messageObject.getQueryParams().get(Constant.SEQ_PROP));
    Seq seqObj = new Seq(seq, eventName, System.currentTimeMillis());
    if (App.seqTimerTask.contains(seqObj)) {
      isException = true;
      exception =
          new BadRequestException(new Throwable("Duplicate seq " + seq + " event: " + eventName));
      return;
    }
    App.seqTimerTask.addSeq(seqObj);

    // send kafka
    try {
      App.kafkaProducer.sendData(Configuration.kafkaTopic, eventName, this.messageObject.getBody(),
          contentType);
      this.messageObject.setHashKey(StringUtils.md5Java(this.messageObject.getBody()));
    } catch (Exception ex) {
      isException = true;
      exception =
          new InternalServerErrorException(new Throwable("Can not send kafka message"
              + ex.getMessage()));
    }
  }

  /**
   * Write response from http client to client of netty server
   * 
   * @param currentObj
   * @param ctx
   * @return
   */
  private boolean writeResponse(ChannelHandlerContext ctx) {
    logger.info("Write response");
    boolean keepAlive = isKeepAlive(request);
    StringBuilder responseContentBuilder = new StringBuilder();
    responseContentBuilder.append("{\"result\": true, \"hashKey\": \""
        + this.messageObject.getHashKey() + "\"}");
    FullHttpResponse response =
        new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(
            responseContentBuilder.toString(), CharsetUtil.UTF_8));
    logger.info("Final content:" + responseContentBuilder.toString());
    response.headers().set(CONTENT_TYPE, EContentType.JSONType.toString());

    if (keepAlive) {
      response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    // Write the response.
    ctx.writeAndFlush(response);

    return keepAlive;
  }

  /**
   * Write exption for client
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("Exception caught", cause);

    HttpResponseStatus status = INTERNAL_SERVER_ERROR;
    if (cause instanceof BadRequestException) {
      status = BAD_REQUEST;
    } else if (cause instanceof MethodNotAllowedException) {
      status = METHOD_NOT_ALLOWED;
    }
    String content = cause.getMessage();
    FullHttpResponse response =
        new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(content,
            CharsetUtil.UTF_8));

    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

    ctx.writeAndFlush(response);
    ctx.close();
  }
}

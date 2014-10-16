package com.lunex.eventprocessor.input.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.input.InputProcessorLaunch;
import com.lunex.eventprocessor.input.Seq;
import com.lunex.eventprocessor.input.UdpMessageObject;
import com.lunex.eventprocessor.input.exception.BadRequestException;
import com.lunex.eventprocessor.input.exception.InternalServerErrorException;
import com.lunex.eventprocessor.input.utils.Configuration;
import com.lunex.eventprocessor.input.utils.Constant;
import com.lunex.eventprocessor.input.utils.StringUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

/**
 * Netty UDP server handler - process message
 *
 */
public class NettyUDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  Logger logger = LoggerFactory.getLogger(NettyUDPServerHandler.class);

  private UdpMessageObject messageObject;
  private Exception exception;
  private boolean isException = false;
  private DatagramPacket packet;

  @Override
  public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
    this.packet = packet;
    if (packet != null) {
      ByteBuf buf = packet.content();
      if (buf.isReadable()) {
        String udpMessage = packet.content().toString(CharsetUtil.UTF_8);
        logger.info(udpMessage);
        // TODO
        try {
          this.messageObject = new UdpMessageObject();
          String[] arrayMessages = udpMessage.split("\n");
          // process event name
          String eventName = arrayMessages[0];
          String[] arrayEventName = eventName.split(":");
          if (arrayEventName.length == 2) {
            this.messageObject.setEvtName(arrayEventName[1]);
          }
          // process seq
          String seq = arrayMessages[1].trim();
          String[] arraySeq = seq.split(":");
          if (arraySeq.length == 2) {
            this.messageObject.setSeq(Long.valueOf(arraySeq[1].trim()));
          }
          // process content type
          String contentType = arrayMessages[2].trim();
          String[] arrayContentType = contentType.split(":");
          if (arrayContentType.length == 2) {
            this.messageObject.setContentType(arrayContentType[1].trim());
          }
          // process content length
          String contentLength = arrayMessages[3].trim();
          String[] arrayContentLength = contentLength.split(":");
          if (arrayContentLength.length == 2) {
            this.messageObject.setContentLength(Long.valueOf(arrayContentLength[1].trim()));
          }
          // process payload
          this.messageObject.setPayLoad(arrayMessages[4].trim());
          this.messageObject.setPayLoadBytes(arrayMessages[4].trim().getBytes(CharsetUtil.UTF_8));
          // Send kafka
          this.sendKafka(ctx);
        } catch (Exception ex) {
          isException = true;
          exception = new BadRequestException(new Throwable(ex.getMessage()));
          exceptionCaught(ctx, exception);
        }
      } else {
        logger.error("package content can not be read");
        isException = true;
        exception = new BadRequestException(new Throwable("Package content can not be read"));
        exceptionCaught(ctx, exception);
      }
    } else {
      logger.error("package is null");
      isException = true;
      exception = new BadRequestException(new Throwable("Package is null"));
      exceptionCaught(ctx, exception);
    }
  }

  /**
   * Send message to kafka
   * 
   * @param ctx
   */
  private void sendKafka(ChannelHandlerContext ctx) {
    // Validate
    String eventName = this.messageObject.getEvtName();
    if (Constant.EMPTY_STRING.equals(eventName)) {
      isException = true;
      exception = new BadRequestException(new Throwable("Event name is empty"));
      return;
    }

    String payLoad = this.messageObject.getPayLoad();
    if (payLoad == null || Constant.EMPTY_STRING.equals(payLoad)) {
      isException = true;
      exception = new BadRequestException(new Throwable("Payload is empty"));
      return;
    }

    String contentType = this.messageObject.getContentType();
    ContentTypeEnum eContentType = ContentTypeEnum.getContentType(contentType);
    if (eContentType == null) {
      isException = true;
      exception = new BadRequestException(new Throwable("Wrong Content-Type"));
      return;
    } else {
      switch (eContentType) {
        case JSONType:
          if (!StringUtils.isJSONValid(payLoad)) {
            isException = true;
            exception = new BadRequestException(new Throwable("Wrong Content-Type"));
            return;
          }
          break;
        default:
          break;
      }
    }

    Long contentLength = this.messageObject.getContentLength();
    if (this.messageObject.getPayLoad().getBytes().length != contentLength) {
      isException = true;
      exception = new BadRequestException(new Throwable("Wrong Content-Length"));
      return;
    }

    Long seq = Long.valueOf(this.messageObject.getSeq());
    Seq seqObj = new Seq(seq, eventName, System.currentTimeMillis());
    if (InputProcessorLaunch.seqTimerTask.contains(seqObj)) {
      isException = true;
      exception =
          new BadRequestException(new Throwable("Duplicate seq " + seq + " event: " + eventName));
      return;
    }
    InputProcessorLaunch.seqTimerTask.addSeq(seqObj);

    // send kafka
    try {
      InputProcessorLaunch.kafkaProducer.sendData(Configuration.kafkaTopic, eventName,
          this.messageObject.getPayLoad(), eContentType);
      this.messageObject.setHashKey(this.messageObject.getPayLoad());
    } catch (Exception ex) {
      isException = true;
      exception =
          new InternalServerErrorException(new Throwable("Can not send kafka message"
              + ex.getMessage()));
      exceptionCaught(ctx, exception);
    }

    if (isException) {
      exceptionCaught(ctx, exception);
    } else {
      ctx.write(new DatagramPacket(Unpooled.copiedBuffer("{\"result\": true, \"hashKey\": \""
          + this.messageObject.getHashKey() + "\"}", CharsetUtil.UTF_8), this.packet.sender()));
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  /**
   * Response message for client
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error(cause.getMessage());
    ctx.write(new DatagramPacket(Unpooled.copiedBuffer("Error: " + cause.getMessage(),
        CharsetUtil.UTF_8), this.packet.sender()));
  }
}

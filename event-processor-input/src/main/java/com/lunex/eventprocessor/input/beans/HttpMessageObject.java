package com.lunex.eventprocessor.input.beans;

import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

public class HttpMessageObject {

  private HttpMethod method;
  private HttpHeaders header;

  private Map<String, String> queryParams;

  private String body;
  private byte[] bodyBytes;
  private Integer contentLengthInByte;
  private String hashKey;

  public HttpMethod getMethod() {
    return method;
  }

  public void setMethod(HttpMethod method) {
    this.method = method;
  }

  public HttpHeaders getHeader() {
    return header;
  }

  public void setHeader(HttpHeaders header) {
    this.header = header;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Map<String, String> getQueryParams() {
    return queryParams;
  }

  public void setQueryParams(Map<String, String> queryParams) {
    this.queryParams = queryParams;
  }

  public HttpMessageObject() {
    this.queryParams = new HashMap<String, String>();
  }

  public Integer getContentLengthInByte() {
    return contentLengthInByte;
  }

  public void setContentLengthInByte(Integer contentLengthInByte) {
    this.contentLengthInByte = contentLengthInByte;
  }

  public byte[] getBodyBytes() {
    return bodyBytes;
  }

  public void setBodyBytes(byte[] bodyBytes) {
    this.bodyBytes = bodyBytes;
  }

  public String getHashKey() {
    return hashKey;
  }

  public void setHashKey(String hashKey) {
    this.hashKey = hashKey;
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("method: " + this.method.toString() + ", ");
    builder.append("header content type: " + this.header.get(CONTENT_TYPE) + ", ");
    builder.append("header content length: " + this.header.get(CONTENT_LENGTH) + ", ");
    builder.append("queryParams: " + this.queryParams.toString() + ", ");
    builder.append("body: '" + this.body + "'");
    return builder.toString();
  }  
}

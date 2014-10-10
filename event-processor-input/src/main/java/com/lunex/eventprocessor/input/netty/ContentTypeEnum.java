package com.lunex.eventprocessor.input.netty;

public enum ContentTypeEnum {

  JSONType("application/json");
  private String contentType;

  private ContentTypeEnum(String stringVal) {
    contentType = stringVal;
  }

  public String toString() {
    return contentType;
  }

  public static ContentTypeEnum getContentType(String verd) {
    if (verd == null) {
      return null;
    }
    for (ContentTypeEnum e : ContentTypeEnum.values()) {
      if (verd.equalsIgnoreCase(e.contentType))
        return e;
    }
    return null;
  }
}

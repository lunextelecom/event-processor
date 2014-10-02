package com.lunex.eventprocessor.input.enums;

public enum EContentType {

  JSONType("application/json");
  private String contentType;

  private EContentType(String stringVal) {
    contentType = stringVal;
  }

  public String toString() {
    return contentType;
  }

  public static EContentType getContentType(String verd) {
    if (verd == null) {
      return null;
    }
    for (EContentType e : EContentType.values()) {
      if (verd.equalsIgnoreCase(e.contentType))
        return e;
    }
    return null;
  }
}

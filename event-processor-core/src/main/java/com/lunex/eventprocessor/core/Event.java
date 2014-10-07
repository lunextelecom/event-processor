package com.lunex.eventprocessor.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Event implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -3901685029349769059L;
  
  long time;
  String name;
  Map<String, Object> event = new HashMap<String, Object>();
}

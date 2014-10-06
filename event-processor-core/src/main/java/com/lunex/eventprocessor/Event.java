package com.lunex.eventprocessor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jerryj on 9/26/14.
 */
public class Event implements Serializable {

  long time;
  String name;
  Map<String, Object> event = new HashMap<String, Object>();
}




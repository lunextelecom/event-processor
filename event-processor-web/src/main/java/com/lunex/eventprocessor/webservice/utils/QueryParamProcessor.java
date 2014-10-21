package com.lunex.eventprocessor.webservice.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class QueryParamProcessor {

  public static Map<String, Object> getEventParam(HttpServletRequest httpRequest) {
    Map<String, Object> result = new HashMap<String, Object>();
    Map<String, String[]> map = httpRequest.getParameterMap();
    Iterator<?> keys = map.keySet().iterator();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      String[] temp = key.split(".");
      if (temp.length == 1) {
        result.put(key, map.get(key)[0]);
      } else if (temp.length == 2) {
        switch (temp[0]) {
          case "int":
            result.put(temp[1], Integer.valueOf(map.get(key)[0]));
            break;
          case "long":
            result.put(temp[1], Long.valueOf(map.get(key)[0]));
            break;
          case "float":
            result.put(temp[1], Float.valueOf(map.get(key)[0]));
            break;
          case "double":
            result.put(temp[1], Double.valueOf(map.get(key)[0]));
            break;
          default:
            result.put(temp[1], map.get(key)[0]);
            break;
        }
      }
    }
    return result;
  }
}

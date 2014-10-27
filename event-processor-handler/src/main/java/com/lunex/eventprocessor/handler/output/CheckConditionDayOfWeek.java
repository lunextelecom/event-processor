package com.lunex.eventprocessor.handler.output;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.ResultComputation;
import com.lunex.eventprocessor.core.dataaccess.CassandraRepository;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.JsonHelper;
import com.lunex.eventprocessor.core.utils.StringUtils;
import com.lunex.eventprocessor.handler.utils.Configurations;

public class CheckConditionDayOfWeek implements CheckConditionHandler {

  static final Logger logger = LoggerFactory.getLogger(CheckConditionDayOfWeek.class);

  public EventResult checkCondition(Map<String, Object> properties, EventQuery eventQuery,
      String hashKey) {
    String eventQueryCondition = eventQuery.getConditions();
    if (eventQueryCondition != null && !Constants.EMPTY_STRING.equals(eventQueryCondition) && eventQuery.getWeight() != null) {
      if (!properties.keySet().isEmpty()) {
        Iterator<String> keys = properties.keySet().iterator();
        while (keys.hasNext()) {
          String key = keys.next();
          String newkey = StringUtils.revertSingleField(key);
          eventQueryCondition =
              eventQueryCondition.replace(newkey, String.valueOf(properties.get(key)));
        }
      }
      long eventTime = (long) properties.get("time");
      long dateMiliseconds = 24*60*60*1000;
      double avgAmount = 0.0;
      double totalAmount = 0.0;
      int numWeek = 0;
      for(int i = 1; i <= 4; i++){
        long passTime =  eventTime - dateMiliseconds*i*eventQuery.getWeight();
        long startTime = getBeginTime(passTime);
        long endTime = getEndTime(passTime);
        try {
          List<ResultComputation> lst = CassandraRepository.getInstance(Configurations.cassandraHost,
              Configurations.cassandraKeyspace).getResultComputation(eventQuery.getEventName(), eventQuery.getRuleName(), startTime, endTime, 1);
          if(lst != null && lst.size()>0){
            ResultComputation resultComputation = lst.get(0);
            if(resultComputation.getResult().get("amount") != null && Double.parseDouble(resultComputation.getResult().get("amount").toString())>0){
              totalAmount = Double.parseDouble(resultComputation.getResult().get("amount").toString());
              numWeek++;
            }
          }
        } catch (Exception e) {
        }
      }
      if(numWeek>0){
        avgAmount = totalAmount/numWeek;
      }
      if(avgAmount==0.0){
        EventResult eventResult =
            new EventResult(eventQuery.getEventName(), String.valueOf(hashKey),
                "{\"result\": false, \"exception\": \"" + "avgAmount = 0" + "\", \"reult\": \""
                    + eventQuery.getRuleName() + "\"}", null);
        return eventResult;
      }else{
        //
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        try {
          // check condition for EventQuery
          eventQueryCondition = eventQueryCondition.replace("avgval", String.valueOf(avgAmount));
          boolean checked = (Boolean) engine.eval(eventQueryCondition);
          logger.info("Result:" + checked + " - " + properties.toString());
          properties = StringUtils.revertHashMapField(properties);
          String jsonStr = JsonHelper.toJSonString(properties);
          // create result with filter result is null
          EventResult eventResult =
              new EventResult(eventQuery.getEventName(), String.valueOf(hashKey), "{\"result\": "
                  + checked + ", \"result-event\": " + jsonStr + ", \"rule\":\""
                  + eventQuery.getRuleName() + "\"}", null);
          return eventResult;
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
          // create result with message error
          EventResult eventResult =
              new EventResult(eventQuery.getEventName(), String.valueOf(hashKey),
                  "{\"result\": false, \"exception\": \"" + e.getMessage() + "\", \"reult\": \""
                      + eventQuery.getRuleName() + "\"}", null);
          return eventResult;
        }
      }
      
    }
    return null;
  }
  public static void main(String[] args) {
    long currentDateTime = System.currentTimeMillis();
    System.out.println(currentDateTime);
  
    //Converting milliseconds to Date using Calendar
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(1413738000085l);
    cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 0, 0, 0);
    System.out.println(cal.getTime());
    
    System.out.println(currentDateTime);
    long t = 1414141281521l;
    System.out.println("1414141281521");
//    1414141281521
    
  }
  
  private long getBeginTime(long time) {
    //Converting milliseconds to Date using Calendar
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(time);
    cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 0, 0, 0);
    return cal.getTime().getTime();
  }
  
  private long getEndTime(long time) {
    //Converting milliseconds to Date using Calendar
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(time);
    cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 23, 59, 59);
    return cal.getTime().getTime();
  }
}

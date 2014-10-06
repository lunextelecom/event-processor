package com.lunex.eventprocessor.handler;


import com.lunex.eventprocessor.EventQuery;
import com.lunex.eventprocessor.QueryFuture;
import com.lunex.eventprocessor.ResultListener;

/**
 * Created by jerryj on 10/2/14.
 * Setup KafkaReader
 * Setup EsperProcessor
 *
 */
public class Main {


  public static void main(String[] argv){
    //sample here
    //should pass in configuration

    EventReader reader = new KafkaReader();

    Processor processor = new EsperProcessor();
    EventQuery query = new EventQuery();
    QueryFuture rs = processor.addQuery("new_order", query);
    processor.bindOutput(rs, new ResultListener[] {new ConsoleOutput()});


    reader.read(processor);



  }



}

package com.lunex.eventprocessor.handler;


import com.lunex.eventprocessor.EventQuery;
import com.lunex.eventprocessor.QueryHierarchy;
import com.lunex.eventprocessor.ResultListener;

/**
 * Created by jerryj on 10/2/14.
 * Setup KafkaReader
 * Setup EsperProcessor
 *
 */
public class Main {


  public static void main(String[] argv){
    //sample here, this code should be put into factory function
    //should pass in configuration for KafkaReader
    //EventQuery should be read from datastore

    EventReader reader = new KafkaReader();

    Processor processor = new EsperProcessor();
    EventQuery query = new EventQuery();
    QueryHierarchy hierarchy = new QueryHierarchy();
    hierarchy.addQuery("new_order", query, new ResultListener[] {new ConsoleOutput()});
    processor.setHierarchy(hierarchy);

    reader.read(processor);

  }



}

event-processor-handler
=======================

### Interface and Classes

Event: an object representing a time event  

EventReader: an Interface - Source of event  

EventConsumer: an Interface - A consumer of event  

KafkaReader: an implement of EventReader - An EventReader that reads from Kakfa topic and convert the message into Event object and pass it into a EventConsumer  

QueryHierarchy: Relate event to query to output  

Processor: an Interface extends from EventConsumer - process an QueryHiearchy by consume event and bind out through QueryHiearchy

EsperProcessor: an implementation of Processor that use Esper

EventProperty: an object represent propeties in event with datatype of property. Using for Processor(EsperProcessor)

EventQuery: an object represent rule

EventQueryException: an object represent rule exception

EventResult: an object represent result after checking condition

QueryFuture: result of one event query

ResultComputation: result of continuous query

### Flow
```
Event 	-> 	Processor 	-> 	ResultListener							
```



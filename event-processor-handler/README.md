event-processor-handler
=======================

### Interface and Classes

Event: a object representing a time event  
EventReader: Source of event  
EventConsumer: A consumer of event  
KafkaReader: An EventReader that reads from Kakfa topic and convert the message into Event object and pass it into a EventConsumer  
QueryHierarchy: Relate event to query to output  
Processor: process an QueryHiearchy  
EsperProcessor: an implementation of Processor that use Esper  

Flow
```
Event 	-> 	Processor 	-> 	ResultListener							
```



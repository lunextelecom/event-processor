Event-Processor
===============
Analyze stream of data events(json, keyvalue, maps) and trigger an action when conditions are met.
Computation is done via aggregation, and time series analysis in Storm.  Storage is provide via Cassandra.


##Summary

[Diagram of Architecture](https://www.draw.io/?url=https%3A%2F%2Fraw.githubusercontent.com%2Flunextelecom%2Fevent-processor%2Fmaster%2Feventprocessor.xml)

````
Input Events ---> Event Processor ---> Response/Action


Event Processor is consist of the following

REST Webservice: Allow client to manually call check or input event data via webservice
InputBuffer: Multi protocol support, buffering/queuing
Data: storage of raw data, result, timeseries, rules
Handler: the engine where raw data feeds in and computing starts here.    	 	
    Continuous Query: declarative way and functional to build incremental computing timeseries. Will use Esper.    
    Output Handler: console, kafka, display data    	
Display: Handle by grafana via kairosdb protocol

````


## Task
```
[ ] = Not done yet
[S] = Specification, not yet implemented
[X] = Complete

[X] Evaluate Influxdb.  Cluster Setup.  Replacing cluster node.
[X] Cassandra with timeseries.  Works good.  Already use inhouse
[X] Define use case
[X] Build phase 1 prototype with Influxdb
    [X] Rule design
    [X] UDP, Http input (Netty)
    [X] Continuous Query, backfill, storage - influxdb already have this feature, incorp rules    
[ ] Build phase 2 Final    
    [ ] Evalute Esper.  
      Change query.  Does it hold states in the case of process restart or server crash?
      Benchmark using basic rule
    [ ] Components
        [S] Input Buffer - Netty
        [ ] Handler
            [ ] Continuous Query(library or use esper)
            [ ] Output Handler            
        [ ] Data
            [ ] Rule
            [ ] Kairos
            [ ] Input, Result, Result Filter
        [ ] Display - Grafana ploting timeseries(kairosdb), Results(Annotation)
        [ ] Web Service - api to access system
        	[ ] Admin for Rule
            [ ] check, add event(proxy to handler)
    [ ] Backfill logic

    [X] Storm (Moved to Phase 3).  Use storm as load distributor across multiple server.    
[ ] Phase 3
    [ ] Storm Integration
    [ ] More customized chart
```

## Interaction with system
* client send event only.  it does not care about result, so it also don't need to know about rule.  
  Should use UDP or HTTP input
* client that want to check on a condition but does not send data (must pass in rule identifier).
  Should use REST API
* client send data and also want to check (must pass in rule identifier).
  Should use REST API
* client that want to react to an event.  Use any of the output handler, eg Kafka.
* client that want to import alot of event/result . Use Kakfa output(up to 1 week old) or read straight from cassandra.

```
Send Event only
Client Produce Data ---UDP/Http-->  InputBuffer --> Event Processor ---Result, TimeSeries--> Storage

Condition Check
Client  	---Check()--> Rest Webservice
			<--Response-- 
```
## Components
### Input Buffer - Asynchonrized Input
Netty service (Standalone application) that read input data and write to kafka topic.  Do not do any serialization.
* Multi-Protocol support UDP, Http
* Buffering - Write result in Kakfa
* Configuration

```
#list of node of the kafka cluster to write to
kafka_clusters: node1,node2,node3
#the name of the topic that will write to
topic_name: event-topic
```
* Input format (require evtname, content-type )
  - evtname: name of event (use for possible load distribution
  - content-type: application/json(just this for now)

### REST Webservice - Provide as api access.
Dropwizard standalone web application.  Provide the following functionality
* Admin api to update rules (proxy to data)

```
Programmer TODO
```

* App api to check, add event (proxy to handler)

```
#add event to system, return a key that can be used to check data
function string addevent(event) 
POST /event?evtname=&parm1=&param2=..

# check data, either entire event can be pass in which or the key
function bool check(event)
function bool check(key)
GET /event?evtname=&parm1=&param2=.. or just pass id=

#same above with result=true mean to wait for result.
function bool add_and_check(event)
POST /event?evtname=&parm1=&param2=..&result=true
```
### Handler
Handler will handle the actual work of computing result, saving display data.

Life Cycle
1. Startup, read rules from database, load into esper runtime
2. Read event from Kafka Topic
3. Process rule for each incoming event that assign to that rule.
  - Esper Filter event  
  - Check Condition  
4. Write result to output
5. Write display data

Note:  If rule is change, update esper runtime and make sure no event are skip.  

###### Backfill
In the situation where the query is changed, we should rerun the query up the the largest timeseries size in the query.  Optionally, can specific how far to go back.

### Rule
Compose of query and check condition. Using queries, a result timeseries can be generated.  from that a numberic threshold value can be compared.

###### Continuous Query - Esper/Complex Event Processing
Incremental computation can be accomplish using continuous query.  The implement is done via Esper library.  Esper allow us to filter and aggegrate moving data or stream of events.

Query should be saved in database not as query, but as Data, Filter, Field, AggregateField.  At runtime, it can be converted to EPL runtime for processing.

```
SELECT [Fields] FROM [Data] WHERE [Filter] GROUP BY [AggregateField]

Query Parts
Data: raw incoming data or generated time series 
AggregateField: Fields that are used to build aggregation of data.
  Timeseries is a special function here to group data into timeseries.
Filter: conditions to filter data. =, !=, >=, <=, >, <, and, or 
Field: field1, field2, or * for all
Field func: sum, max, min, first, last, avg, timeseries(timefield, size1, size2[optional])
```

###### Condition
Condition is code executed base on the data output from the continuous query.  It truncate the resulting data into a bool(Result)
When this condition is met, check will return true.

Condition Exception: There will also be special case where the regular condition should not be applied. 

Example:  
```
Save exception in Storage {action: verified, entity: A, rule: 'rule1', expireddate: '08/14/2014'} as a Output Filter
So, rule 1 is not applied for event of entity A from now to 08/14/2014, and result after filtered will be processed by output handle and saved in storage
```
###### Output (Java Code)
Output specfic where the computed result of Condition should go.  By default all output should be saved to database.

In some case the application that want to receive events of pattern match might not be the one sending the data.  To recieve notification of those event, clients can subscript to Kafka topic.

### Data (Java Code)
Abstract Data access
Store the following
1. Raw Input Event
2. Timeseries(KairosDb)
3. Result, Filtered Result. Some of these might need to be copied to KairosDB.
4. Rule(Query Parts, Condition, Filtered Condition, Output)

### Display (Javascript, Browser)
Graph will be handle by grafana.  Data for the graphie will be consist of timeseries data and result which are stored in KairosDB.  Timeseries will be graphic as chart, while the result as annotation in the chart.

## Use Case
Allow only 100 order per mins for seller=usedcardealer
Allow only $10000 sales daily for any seller

###### Synchronized
```
1. ClientApp send request add_and_check(evt=neworder, amount=10, seller=usedcardealer) to EventProcessor(blocked)
2. EventProcessor add the new event, incremental computing base on the new data, and return the result.
3. ClientApp recieve response(finished)

or on high volume deployment

1. ClientApp send request add_and_check(evt=neworder, amount=10, seller=usedcardealer) to EventProcessor(blocked)
2. EventProcessor add event to queue and lookup already computed result.
3. ClientApp recieve response(finished)

```
###### Asynchronized
Asynchronized use case(make sense only if the condition dealing with larger number of events, because we can potentially some event that are not process yet)
```
1. Many ClientApp send UDP event (evt=neworder, amount=10..) to EventProcessor.(Non blocking)
2. EventProcessor update internal data, recompute incrementally.
3. ClientApp send request check_only(evt=neworder, amount=10, seller=usedcardealer) to EventProcessor(blocked)
4. EventProcessor return already computed result.
5. ClientApp recieve response(finished)
```
###### Custom handler
```
Prerequisite:
Make sure rule is configure and that there is a Check condition matches and that output is set to queue

Setup:
1. Have program or script subscript the that rabbitmq topic
2. When event are match, rabbitmq will publish the topic, the program will then receive, evtname, rulename, the condition that cause the match.
Note: the data is feed by other applications.
```


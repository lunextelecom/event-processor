Event-Processor
===============
Analyze stream of data events(json, keyvalue, maps) and trigger an action when conditions are met.
Computation is done via aggregation, and time series analysis in Storm.  Storage is provide via Cassandra.


##Summary

[Diagram of Architecture](https://www.draw.io/?url=https%3A%2F%2Fraw.githubusercontent.com%2Flunextelecom%2Fevent-processor%2Fmaster%2Feventprocessor2.xml)

````
Input Events ---> Event Processor ---> Response/Action


Event Processor is consist of the following

REST Webservice: Allow client to manually call check or input event data via webservice
Input Buffer: Multi protocol support, buffering/queuing
Data: storage of raw data, result, timeseries, rules
Handler: the engine where raw data feeds in and computing starts here.    	 	
Display: Handle by grafana via kairosdb protocol

````


## Task
```
[ ] = Not done yet
[S] = Specification, not yet implemented
[W] = Work in progress
[X] = Complete

[X] Evaluate Influxdb.  Cluster Setup.  Replacing cluster node.
[X] Cassandra with timeseries.  Works good.  Already use inhouse
[X] Define use case
[X] Build phase 1 prototype with Influxdb
    [X] Rule design
    [X] UDP, Http input (Netty)
    [X] Continuous Query, backfill, storage - influxdb already have this feature, incorp rules    
[ ] Build phase 2 Final    
    [X] Evalute Esper. Benchmark using basic rule
    [ ] Components
        [X] Input Buffer - Netty
        [W] Core
        	[ ] Rule			     
            [ ] Data Access
            	[ ] Rule            
            	[ ] Input, Result
        [W] Handler
            [ ] Continuous Query(library or use esper)
            [ ] Output Handler            
        [ ] Display - Grafana ploting timeseries(kairosdb), Results(Annotation)
        	[ ] Kairos
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
1. Add new rule:
POST /admin/rule?evtName=new_order&ruleName=rule10
Body in json format
{"evtName":"new_order", //name of event
"ruleName":"rule10", //name of rule
"data":"new_order", //name of data, the same with name of event, which provide for esper processor, using in 'from' of rule
"fields":"sum(amount:double), count(txId:long), acctNum:string", //selected fields of query rule
"bigBucket":"5 minute", //big bucket is time for last timeframe
"smallBucket":"10 second", //small bucket is time for every timeframe
"conditions":"sum(amount)>120", //condition to check result
"aggregateField":"acctNum:string", //'group by' clause in rule
"filters":"amount:double > 10 and acctNum:string = 'PC01D005'", //using in 'where' clause
"having":"sum(amount:double) > 20",//using in 'having' clause of query rule
"type": 0, //type of rule, currently, nomarl:0 and day_of_week:1
"weight": 0, //how far to check day_of_week
"description": "description", 
"autoStart": false, //auto start rule when add
"backfill": false, //auto start backfill when start
"backfillTime": "1 day" //how far to backfill
}

2. Delete rule:
DELETE /admin/rule?evtName=new_order&ruleName=rule10

3. Update rule:
PUT /admin/rule?evtName=new_order&ruleName=rule1
Body in json format
{"evtName":"new_order", //name of event
"ruleName":"rule10", //name of rule
"data":"new_order", //name of data, the same with name of event, which provide for esper processor, using in 'from' of rule
"fields":"sum(amount:double), count(txId:long), acctNum:string", //selected fields of query rule
"bigBucket":"5 minute", //big bucket is time for last timeframe
"smallBucket":"10 second", //small bucket is time for every timeframe
"conditions":"sum(amount)>120", //condition to check result
"aggregateField":"acctNum:string", //'group by' clause in rule
"filters":"amount:double > 10 and acctNum:string = 'PC01D005'", //using in 'where' clause
"having":"sum(amount:double) > 20",//using in 'having' clause of query rule
"type": 0, //type of rule, currently, nomarl:0 and day_of_week:1
"weight": 0, //how far to check day_of_week
"description": "description", 
"autoStart": false, //auto start rule when add
"backfill": false, //auto start backfill when start
"backfillTime": "1 day" //how far to backfill
}

4. Stop rule:
PUT /admin/rule/stop?evtName=new_order&ruleName=rule1


5. Start rule: 
PUT /admin/rule/start?evtName=new_order&ruleName=rule1&backfill=true&backfillTime=1 day
Params:
evtName: name of event
ruleName: name of rule
backfill: true/false
backfillTime: time to backfill (N day, N hour, N minute, N second)

6. Add rule exception config:
POST /rule-exception?evtName=new_order&ruleName=rule1&action=verified&expiredDate=21/10/2014 15:00:00
Params:
evtName: name of event
ruleName: name of rule
action: currently ussing verified
expiredDate: time to apply rule exception(dd/MM/mmmm HH:mm:ss)
Body in json format
{"acctNum": "PC01D001"} // fields of rule which need to be skipped

```

* App api to check, add event (proxy to handler)

```
## Add event to system, return a key that can be used to check data result
POST /event?evtname=&result=
- Params:
evtName: name of event
result: true/false(true: wait and get result, false: async)
- Body in json format:
{
"evtName": "new_order", // name of event(required)
"time" : 1435235124124, // time in ms(required)
"acctNum": "PC01D001", // properties of event object
"amount": 135.0, ....
}
- Response:
In json format
{
"result": true/false,
"message": "" // response message, return list violation if param result = true, return hashkey if param result = false
}

## check data, either entire event can be pass in which or the key
#check data by key
GET /event?hashKey=
- Params:
hashKey: return key when add new event
- Response:
In json format
{
"result": true/false,
"message": "" // response message, return list violation
}

# check data by event
POST /event/check?evtname=&result=
- Params:
evtName: name of event
result: true/false(true: wait and get result, false: async)
- Body in json format:
{
"evtName": "new_order", // name of event(required)
"time" : 1435235124124, // time in ms(required)
"acctNum": "PC01D001", // properties of event object
"amount": 135.0, ....
}
- Response:In json format
{
"result": true/false,
"message": "" // response message, return list violation
}

```
### Handler
Handler will handle the actual work of computing result, saving display data.  Handler is a standalone application that reads from Kakfa.  There will be n number of Handler running to match the number of Kakfa partitions.

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

### Core
Core library and function.

1. Rule 
2. Data Access

###### Rule 
Compose of query and check condition. Using queries, a result timeseries can be generated.  from that a numberic threshold value can be compared.

* Continuous Query - Esper/Complex Event Processing
Incremental computation can be accomplish using continuous query.  The implement is done via Esper library.  Esper allow us to filter and aggegrate moving data or stream of events.

Query should be saved in database not as query, but as Data, Filter, Field, AggregateField.  At runtime, it can be converted to EPL runtime for processing.

```
SELECT [Fields] FROM [Data] WHERE [Filter] GROUP BY [AggregateField] HAVING [Having]

Query Parts
Data: raw incoming data or generated time series 
AggregateField: Fields that are used to build aggregation of data.
  Timeseries is a special function here to group data into timeseries.
Filter: conditions to filter data. =, !=, >=, <=, >, <, and, or 
Field: field1, field2, or * for all
Field func: sum, max, min, first, last, avg, timeseries(timefield, size1, size2[optional])
```
```
Format:
[Fields] - "sum(amount:double), count(txId:string), txId:string, reseller:string"
[Data] - "new_order"
[Filter] - "reseller:string = 'resellet1' and txId:string = 'transactionId1' and amount:double > 20.0"
[AggregateField] - "reseller:string, txId:string"
[Having] - "sum(amount:double) > 50.0"
* we need datatype for properties of event to register event with esper
```
* Condition
Condition is code executed base on the data output from the continuous query.  It truncate the resulting data into a bool(Result)
When this condition is met, check will return true.
```
Format for condition:
sum(amount) > 100 && count(txId) > 30
```

Condition Exception: There will also be special case where the regular condition should not be applied. 

Example:  
```
Save exception in Storage {action: verified, entity: A, rule: 'rule1', expireddate: '08/14/2014'} as a Output Filter
So, rule 1 is not applied for event of entity A from now to 08/14/2014, and result after filtered will be processed by output handle and saved in storage
```
* Output 
Output specfic where the computed result of Condition should go.  By default all output should be saved to database.

In some case the application that want to receive events of pattern match might not be the one sending the data.  To recieve notification of those event, clients can subscript to Kafka topic.

Timeframe for event - we have 2 type of time frame
* Every timeframe(truncate or not truncate)
 - Not truncate every timeframe: get event in every n time(every 6 minute and next evry 6 minute, ex: with 1 hour, now is 7:15, 1st every timeframe is 7:15-8:14, 2nd every timeframe is 8:15-9:14 ....)
 - Truncate every timeframe: get event in every n time, but truncate at start time(ex: with every 1 hour, now is 7:15, 1st every timeframe is 7:00-7:59, 2nd every timeframe is 8:00-8:59)
 - Using smallbucket(ex: 1 hour) to get event in every timeframe
* Last timeframe: get event in last n time(last 4 hour, 6 minute)
 - Using smallbucket(ex: 10s) to get event in every timeframe
 - Using bigbucket(ex: 4 hour) to get event in last timeframe base on every 10s timeframe
 
 In case every timeframe, bigbucket is null



###### Data Access
It is very important to keep data storage consistent as in future, we can swap in Storm for load distribution.
Store the following

1. Raw Input Event
2. Timeseries(KairosDb)
3. Result, Filtered Result. Some of these might need to be copied to KairosDB.
4. Rule(Query Parts, Condition, Filtered Condition, Output)

### Display
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


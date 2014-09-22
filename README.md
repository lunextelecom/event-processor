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
Input: Multi protocol support, buffering/queuing
Storage: storage of raw data, result, timeseries, rules
Event Processor: the engine where raw data feeds in and computing starts here.
    Event Handler: entry point for event. do work such as determine rules, coordinate with output.
    Continuous Query: declarative way and functional to build incremental computing timeseries. Will use Esper.
    Output filter: filter rule which apply for event.  
    Rule: define the query, condition, and output, output filter
    Output Handler: console, kafka, display data(kariosdb phase 2 only)
Graph: Handle by grafana via kairosdb protocol

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
        [ ] Processor
            [ ] Continous Query(library or use esper)
            [ ] Output Handler
        [ ] Data
            [ ] Rule
            [ ] Kairos
            [ ] Input, Result, Filter Result
        [ ] Display - Grafana ploting timeseries(kairosdb), Results(Annotation)
        [ ] Web Service
    [ ] Backfill logic

    [X] Storm (Moved to Phase 3).  Use storm as load distributor across multiple server.    
[ ] Phase 3
    [ ] Storm Integration
    [ ] More customized chart
```

```
Flow of Events Phase 2

Client Produce Data --UDP/Http-->  InputBuffer  -> Event Processor -Result, TimeSeries-> Storage
```

##Performance
###### Incremental computation
Computation should be incremental to reduce CPU and IO.  This means intermediate result can be saved in memory or in database so it can be reuse for later use.  In the case where incremental is not possible, it should use precomputed time series data instead of the raw data set.  

Example: let say we are interested in the last order total in the last 60 mins.  It would be inefficient to query the last 60 mins of order each time if we have order coming in at 100/min since on this mean we might be adding up 6000 records 100 times per min.  Instead we can save use a times series of 10 seconds.  So that means we are just adding 60 record 6x per min.  That is a drop from 6000x100 = 600,000 to 360 operation per min.

###### Downsample using timeseries
By downsample raw data into time series, it is alot easier to visualized and run function on the data.

###### Drawback
1. Well there is no free lunch, there is some extra work require to build the time series.  Also, with time series we dealing with a data granual of 10 sec although 1 sec can be use.

2. Backfilling.  When conditions are change, recomputing old data is required.  How far to to backfill will depend in the logic and query.  In the above example, we would need to recompute the last 60 mins.

3. Granularity lost with timeseries

###### Using multiple timeseries together to have better response and granularity.
```
Example:  Want to know the avg value for the last 60 mins with 10 sec granularity.  In there we will build 2 series. First one is bucket of 1 sec while the larger one is 1 min.

at time 0
[bucket0]...[bucket359]  #Granular bucket.  Times series1 @ 10 sec (this can be graph)
[      60min          ]  #Custom bucket Times series2 @ 60 mins. (do not graph this as it mutate)

at time 10sec, bucket360 is added.  bucket0-359 is already computed, so no waste cpu.  bucket360 is incremental added.  To find out the avg value of the last 60 mins, we can just minus bucket0 and add bucket360

[bucket0][bucket1]...[bucket360]  #Times series1 10 sec
         [      60min          ]  #Times series2 60 min

and some time later...
[bucket2]...[bucket361][bucket362]...[bucket721]
            [      60min            ] #notice always only 1 bucket

*Note, the 60min bucket might not be possible to do within the continuos query of influxdb.  We have to manually compute that. 

```

###### Interaction with system
* client send event only.  it does not care about result, so it also don't need to know about rule.  
  Should use UDP or HTTP input
* client that want to check on a condition but does not send data (must pass in rule identifier).
  Should use REST API
* client send data and also want to check (must pass in rule identifier).
  Should use REST API
* client that want to react to an event.  Use any of the output handler, eg AMQP.
* client that want to import alot of event/result . Use Kakfa output(up to 1 week old) or read straight from cassandra.

###### Input
* Asynchronized input where response are not needed
UDP - 
Http (params, json) - Clients 
Kafka



###### REST Webservice

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
###### Continuous Query
One method of doing incremental computation is by using Continuous query.  Continuous query operates on streaming dataset/timeseries and recompute incrementally on each new sample data.  

```
SELECT [Fields] FROM [Data] WHERE [Filter] GroupBy [AggregateField]
CHECK [Condition] OUTPUT [CONSOLE | LOG | QUEUE]

Data: raw incoming data or genearted time series 
AggregateField: Fields that are used to build aggregation of data.
  Timeseries is a special function here to group data into timeseries.
Filter: conditions to filter data. =, !=, >=, <=, >, <, and, or 
Field: field1, field2, or * for all
Field func: sum, max, min, first, last, avg, timeseries(timefield, size1, size2[optional])
Condition: when this condition is met, check will return true.  Also, it can be set to write an event to rabbitmq.
```

Example query:
```
let's say raw data is 
{time, symbol, price, size}

1.
#truncate raw data into 5 mins
series@5min = select start(time), symbol, price from raw group by timeseries(time, 5min), symbol
last30min@5min = select * from series_5min(30min) 

2.
last30min@5min = select start(time), symbol, price from raw group by timeseries(time,5min, 30min), symbol

3.
last30min@5min = select start(time), symbol, price from raw group by timeseries(time,5min, 30min), symbol
```

Example query and check:
```
Allow only 5 failed login within 10 mins
{username, code, time}
SELECT count FROM login_event(10sec, 10mins) WHERE login_event.code < 0 GROUP BY username

#the condition for check. This also generate an event.
CHECK count >= 5 

```

###### Backfill
In the situation where the query is changed, we should rerun the query up the the largest timeseries size in the query.  Optionally, can specific how far to go back.

###### Output
In some case the application that want to receive events of pattern match might not be the one sending the data.  To recieve notification of those event, clients can subscript to Kafka topic.

Output handler will be responsible for converting the computation timeseries(eg. 10sec) into the display data in KairosDB format(10sec, 1min, 5 min, 1 hour).


Example:
Output Filter: filter rule which apply for event to create filtered result

Save exception in Storage {action: verified, entity: A, rule: 'rule1', expireddate: '08/14/2014'} as a Output Filter

So, rule 1 is not applied for event of entity A from now to 08/14/2014, and result after filtered will be processed by output handle and saved in storage

###### Graphing
Graph will be handle by grafana.  Both the immuntable timeseries and event will be stored and graphed.  
Grafana will need data in 10sec, 1min, 5min, 1 hour bucket size depending on the date range set on the display.  For this reason, the timeseries used for computation and display are different.  The display data are in kairosdb format stored in cassandra and it is also RRD. 


###### Rule
Compose of query and check condition. Using queries, a result timeseries can be generated.  from that a numberic threshold value can be compared.

## UseCase
Allow only 100 order per mins for seller=usedcardealer
Allow only $10000 sales daily for any seller

###### Flow
Synchronized used case.
```
1. ClientApp send request add_and_check(evt=neworder, amount=10, seller=usedcardealer) to EventProcessor(blocked)
2. EventProcessor add the new event, incremental computing base on the new data, and return the result.
3. ClientApp recieve response(finished)

or on high volume deployment

1. ClientApp send request add_and_check(evt=neworder, amount=10, seller=usedcardealer) to EventProcessor(blocked)
2. EventProcessor add event to queue and lookup already computed result.
3. ClientApp recieve response(finished)

```
######
Asynchronized use case(make sense only if the condition dealing with larger number of events, because we can potentially some event that are not process yet)
```
1. Many ClientApp send UDP event (evt=neworder, amount=10..) to EventProcessor.(Non blocking)
2. EventProcessor update internal data, recompute incrementally.
3. ClientApp send request check_only(evt=neworder, amount=10, seller=usedcardealer) to EventProcessor(blocked)
4. EventProcessor return already computed result.
5. ClientApp recieve response(finished)
```
######
Custom trigger for condition that happen.
```
Prerequisite:
Make sure rule is configure and that there is a Check condition matches and that output is set to queue

Setup:
1. Have program or script subscript the that rabbitmq topic
2. When event are match, rabbitmq will publish the topic, the program will then receive, evtname, rulename, the condition that cause the match.
Note: the data is feed by other applications.
```


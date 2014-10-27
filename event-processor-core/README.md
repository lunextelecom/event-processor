event-processor-core
=======================

###DB design:


Rule
```
// Column fammily for rule
CREATE TABLE rules (
 	event_name text, // name of event
 	rule_name text, // name of rule

 	data text, // equal with event_name, using in "from" clause of rule
 	fields text, // using in "select" clause. Format: name:datatype, sum(name:datatype), max(name:datatype)... separate by ",". Ex: acctNum:string, sum(amount:double)
	filters text, // using in "where" clause. Format as fields column
 	aggregate_field text, // using in "group by" clause. Format as fields column
 	having text, // using in "having" clause. Format as fields column
 	small_bucket text, // bucket for every timeframe
 	big_bucket text, // bucket for last timeframe, base on small bucket
	conditions text,// condition to check violation of result. Format as expression: name > value && sum(name) > value || 1 == 1....  Ex: sum(amount) > 30.0 and count(txId) > 5
 	description text,// description for rule
 	status text,// status of rule, RUNNING or STOP
 	type int, // type of rule, 0: default and 1: day of week
 	weight int, // using for case type = 1
	PRIMARY KEY (event_name, rule_name) 
);
 ```
 
 
Event 
 ```
 // Column family for event
 CREATE TABLE events (
	event_name text, // event name
 	time bigint, // time of event, base on milisecond 
 	hashkey text, // hashkey of event, return for client when adding new event
    
 	event text, // properties of event, base on json format. Ex: {"acctNum": "PC01D001", "amount": 120.0}
 	PRIMARY KEY (event_name, time, hashkey)
 ) WITH CLUSTERING ORDER BY (time ASC);
 ```
 
 Result after check condition
  ```
// Column family for result of event after check condition 
 CREATE TABLE results (
	event_name text, // name of event
	hashkey text, // hashkey of event
    
	result list<text>,// result of event after checking condition and no applying exception rule
	filtered_result list<text>,// result of event after applying rule exception
	PRIMARY KEY ( event_name, hashkey)
);
 ```
 
 Rule Exception
  ```
// Column family for rule exception
CREATE TABLE condition_exception (
	id uuid, // unique key
	event_name text, // event name
	rule_name text, // rule name
	action text, // action apply for exception. Currently, 'verified'
	expired_date timestamp,// time to apply this exception
	condition_filter text,// list properties of evnt need to check exception, base on json format as event column in events table. Ex: {"acctNum":"PC01D001"}
	PRIMARY KEY (id, event_name, rule_name, action, expired_date) 
);
 ```
 
 Result of continuous query 
  ```
// Column family for result of continuous query
CREATE TABLE result_computation (
	event_name text,// event name
	rule_name text,// rule name
	time bigint,// time of event create result of continuous query
	hashkey text,// hashKey of event
    
	result text,// result of continuous query, base on json format. Ex: {"acctNum": "PC01D001", "time": 140002040506, "hashKey":"H324jkd3k346klj346l", "sum(amount)": 120.0}
	PRIMARY KEY (event_name, rule_name, time, hashkey) 
) WITH CLUSTERING ORDER BY (rule_name ASC, time DESC);
```


### Object
Review in https://github.com/lunextelecom/event-processor/tree/master/event-processor-handler
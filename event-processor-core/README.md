###DB design:

```
// Column fammily for rule
CREATE TABLE rules (
 	event_name text, // name of event
 	rule_name text, // name of rule

 	data text, // equal with event_name, using in "from" clause of rule
 	fields text, // using in "select" clause
	filters text, // using in "where" clause
 	aggregate_field text, // using in "group by" clause
 	having text, // using in "having" clause
 	small_bucket text, // bucket for every timeframe
 	big_bucket text, // bucket for last timeframe, base on small bucket
	conditions text,// condition to check violation of result
 	description text,// description for rule
 	status text,// status of rule, RUNNING or STOP
 	type int, // type of rule, 0: default and 1: day of week
 	weight int, // using for case type = 1
	PRIMARY KEY (event_name, rule_name) 
);
 
 // Column family for event
 CREATE TABLE events (
	event_name text, // event name
 	time bigint, // time of event, base on milisecond 
 	hashkey text, // hashkey of event, return for client when adding new event
    
 	event text, // properties of event, base on json format
 	PRIMARY KEY (event_name, time, hashkey)
 ) WITH CLUSTERING ORDER BY (time ASC);

// Column family for result of event after check condition 
 CREATE TABLE results (
	event_name text, // name of event
	hashkey text, // hashkey of event
    
	result list<text>,// result of event after checking condition and no applying exception rule
	filtered_result list<text>,// result of event after applying rule exception
	PRIMARY KEY ( event_name, hashkey)
);

// Column family for rule exception
CREATE TABLE condition_exception (
	id uuid, // unique key
	event_name text, // event name
	rule_name text, // rule name
	action text, // action apply for exception. Currently, 'verified'
	expired_date timestamp,// time to apply this exception
	condition_filter text,// list properties of evnt need to check exception, base on json format as event column in events table
	PRIMARY KEY (id, event_name, rule_name, action, expired_date) 
);

// Column family for result of continuous query
CREATE TABLE result_computation (
	event_name text,// event name
	rule_name text,// rule name
	time bigint,// time of event create result of continuous query
	hashkey text,// hashKey of event
    
	result text,// result of continuous query, base on json format
	PRIMARY KEY (event_name, rule_name, time, hashkey) 
) WITH CLUSTERING ORDER BY (rule_name ASC, time DESC);
```
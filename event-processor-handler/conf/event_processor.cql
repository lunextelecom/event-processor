DROP KEYSPACE event_processor;

CREATE KEYSPACE event_processor
WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'dc1' : 2 };

//CREATE KEYSPACE event_processor
//WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE event_processor;

CREATE TABLE rules (
 	event_name text,
 	rule_name text,

 	data text, 
 	fields text,
	filters text,
 	aggregate_field text,
 	having text,
 	small_bucket text,
 	big_bucket text,
	conditions text,
 	description text,
 	status text,
	PRIMARY KEY (event_name, rule_name) 
);
 
 
 CREATE TABLE events (
	event_name text,
 	time bigint,
 	hashkey text,
 	event text,
 	PRIMARY KEY (event_name, time, hashkey)
 ) WITH CLUSTERING ORDER BY (time ASC);

 CREATE TABLE results (
	event_name text,
	hashkey text,
	result list<text>,
	filtered_result list<text>,
	PRIMARY KEY ( event_name, hashkey)
);

CREATE TABLE condition_exception (
	id uuid, 
	event_name text, 
	rule_name text,
	action text,
	expired_date timestamp,
	condition_filter text,
	PRIMARY KEY (id, event_name, rule_name, action, expired_date) 
);

CREATE TABLE result_computation (
	event_name text,
	rule_name text,
	time bigint,
	hashkey text,
	result text,
	PRIMARY KEY (event_name, rule_name, time, hashkey) 
);
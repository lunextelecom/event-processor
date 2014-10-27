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
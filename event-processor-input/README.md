## Input Buffer
1. read input, determine evtname, content-type of payload.  Avoid serializing payload.
2. basic duplicate rejection
3. determine kafka topic to write
4. write to kafka

### Message Format
```
#for http, here are the values to send
HTTP
Verb = POST
Parameter:
	evtname #name of event
    seq #incremental number used for this event.  use for dup rejection
Header:   
	Content-Type: #default json(default), msgpack
    Content-Length: #length of payload in byte
Body:
	{Payload}

#For UDP, here is the message format
evtname: \n
seq: \n
Content-Type: \n #default json
Content-Length: \n # default 0
\n
{Payload}
\n

```

### Configuration
```
zookeepers = [zk_node1, zk_node2]
kafka_cluster = [node1, node2]
```

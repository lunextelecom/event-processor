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


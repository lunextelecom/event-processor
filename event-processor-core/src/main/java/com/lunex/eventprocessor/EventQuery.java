package com.lunex.eventprocessor;

/**
 * SELECT [Fields] FROM [Data] WHERE [Filter] GROUP BY [AggregateField]
 * Query Parts
 *  Data: raw incoming data or generated time series
 *  AggregateField: Fields that are used to build aggregation of data.
 *  Timeseries is a special function here to group data into timeseries.
 *  Filter: conditions to filter data. =, !=, >=, <=, >, <, and, or
 *  Field: field1, field2, or * for all
 *  Field func: sum, max, min, first, last, avg, timeseries(timefield, size1, size2[optional])
 */
public class EventQuery {

}

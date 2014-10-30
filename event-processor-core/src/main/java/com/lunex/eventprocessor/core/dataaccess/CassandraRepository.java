package com.lunex.eventprocessor.core.dataaccess;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.google.common.base.Strings;
import com.lunex.eventprocessor.core.Event;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.EventQuery.EventQueryStatus;
import com.lunex.eventprocessor.core.EventQuery.EventQueryType;
import com.lunex.eventprocessor.core.EventQueryException;
import com.lunex.eventprocessor.core.EventQueryException.ExptionAction;
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.ResultComputation;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.JsonHelper;

public class CassandraRepository {

  static final Logger logger = LoggerFactory.getLogger(CassandraRepository.class);

  private static CassandraRepository instance = null;
  private Session session;
  private Cluster cluster;
  private String keyspace;
  private Map<String, PreparedStatement> listPreparedStatements;

  /**
   * Get instance
   * 
   * @return
   */
  public static CassandraRepository getInstance(String host, String keyspace) throws Exception {
    if (instance == null) {
      instance = init(host, keyspace);
    }
    return instance;
  }

  /**
   * Init connection
   * 
   * @param serverIP
   * @param keyspace
   * @return
   */
  private static CassandraRepository init(String serverIP, String keyspace) throws Exception {
    instance = new CassandraRepository();
    if (Strings.isNullOrEmpty(keyspace)) {
      instance.keyspace = "event_processor";
    } else {
      instance.keyspace = keyspace.trim();
    }
    Builder builder = Cluster.builder();
    builder.addContactPoint(serverIP);
    PoolingOptions options = new PoolingOptions();
    options.setCoreConnectionsPerHost(HostDistance.LOCAL,
        options.getMaxConnectionsPerHost(HostDistance.LOCAL));
    builder.withPoolingOptions(options);
    instance.cluster =
        builder.withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
            .withReconnectionPolicy(new ConstantReconnectionPolicy(100L)).build();
    instance.session = instance.cluster.connect();
    Metadata metadata = instance.cluster.getMetadata();
    if (metadata != null) {
      KeyspaceMetadata keyspaceMetadata = metadata.getKeyspace(instance.keyspace);
      if (keyspaceMetadata == null) {
        throw new UnsupportedOperationException("Can't find keyspace :" + instance.keyspace);
      }
      if (keyspaceMetadata.getTable("rules") == null) {
        throw new UnsupportedOperationException("Can't find table logging in " + instance.keyspace);
      }
      if (keyspaceMetadata.getTable("events") == null) {
        throw new UnsupportedOperationException("Can't find table endpoint in " + instance.keyspace);
      }
      if (keyspaceMetadata.getTable("condition_exception") == null) {
        throw new UnsupportedOperationException("Can't find table condition_exception in "
            + instance.keyspace);
      }
      if (keyspaceMetadata.getTable("result_computation") == null) {
        throw new UnsupportedOperationException("Can't find table result_computation in "
            + instance.keyspace);
      }
      if (keyspaceMetadata.getTable("results") == null) {
        throw new UnsupportedOperationException("Can't find table result_computation in "
            + instance.keyspace);
      }
    }
    instance.listPreparedStatements = new HashMap<String, PreparedStatement>();
    return instance;
  }

  /**
   * Close connect
   * 
   * @return
   */
  public boolean closeConnection() {
    try {
      session.close();
      cluster.close();
      return true;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return false;
    }
  }

  /**
   * Save raw input event to db
   * 
   * @param event
   * @throws Exception
   */
  public void insertEventToDB(Event event) throws Exception {
    String sql =
        "INSERT INTO " + keyspace
            + ".events (event_name, time, hashkey, event) VALUES (?, ?, ?, ?);";
    List<Object> params = new ArrayList<Object>();
    params.add(event.getEvtName());
    params.add(event.getTime());
    params.add(event.getHashKey());
    params.add(event.getPayLoadStr());
    execute(sql, params);
  }

  public List<Event> getEvent(long startTime) throws Exception {
    String sql = "SELECT * FROM " + keyspace + ".events where time > ? ALLOW FILTERING;";
    List<Object> params = new ArrayList<Object>();
    params.add(startTime);
    ResultSet rows = execute(sql, params);
    List<Event> results = null;
    Event event = null;
    for (Row row : rows) {
      if (results == null) {
        results = new ArrayList<Event>();
      }
      event = new Event(row.getLong("time"), row.getString("event"));
      results.add(event);
    }
    return results;
  }

  public List<Event> getEvent(long startTime, String eventName) throws Exception {
    String sql =
        "SELECT * FROM " + keyspace + ".events where event_name = ? and time > ? ALLOW FILTERING;";
    List<Object> params = new ArrayList<Object>();
    params.add(eventName);
    params.add(startTime);
    ResultSet rows = execute(sql, params);
    List<Event> results = null;
    Event event = null;
    for (Row row : rows) {
      if (results == null) {
        results = new ArrayList<Event>();
      }
      event = new Event(row.getLong("time"), row.getString("event"));
      results.add(event);
    }
    return results;
  }

  public List<Event> getEvent(String hashkey) throws Exception {
    String sql = "SELECT * FROM " + keyspace + ".events where hashkey = ? ALLOW FILTERING;";
    List<Object> params = new ArrayList<Object>();
    params.add(hashkey);
    ResultSet rows = execute(sql, params);
    List<Event> results = null;
    Event event = null;
    for (Row row : rows) {
      if (results == null) {
        results = new ArrayList<Event>();
      }
      event = new Event(row.getLong("time"), row.getString("event"));
      results.add(event);
    }
    return results;
  }

  /**
   * Get event query (rule) from DB
   * 
   * @param id : id=-1 --> skip this field
   * @param eventName
   * @param ruleName
   * @return
   * @throws Exception
   */
  public List<EventQuery> getEventQueryFromDB(String eventName, String ruleName) throws Exception {
    String sql = "SELECT * FROM " + keyspace + ".rules";
    List<Object> params = new ArrayList<Object>();
    if (!Constants.EMPTY_STRING.equals(eventName)) {
      sql += " WHERE event_name = ? ";
      params.add(eventName);
    }
    if (!Constants.EMPTY_STRING.equals(eventName)) {
      if (!Constants.EMPTY_STRING.equals(eventName)) {
        sql += " AND rule_name = ? ";
      } else {
        sql += " WHERE rule_name = ? ";
      }
      params.add(ruleName);
    }
    sql += " ALLOW FILTERING;";
    ResultSet rows = execute(sql, params);
    List<EventQuery> results = null;
    EventQuery eventQuery = null;
    for (Row row : rows) {
      if (results == null) {
        results = new ArrayList<EventQuery>();
      }
      eventQuery = new EventQuery();
      eventQuery.setEventName(row.getString("event_name"));
      eventQuery.setData(row.getString("data"));
      eventQuery.setFields(row.getString("fields"));
      eventQuery.setFilters(row.getString("filters"));
      eventQuery.setAggregateField(row.getString("aggregate_field"));
      eventQuery.setSmallBucket(row.getString("small_bucket"));
      eventQuery.setBigBucket(row.getString("big_bucket"));
      eventQuery.setConditions(row.getString("conditions"));
      eventQuery.setRuleName(row.getString("rule_name"));
      eventQuery.setHaving(row.getString("having"));
      eventQuery.setDescription(row.getString("description"));
      eventQuery.setStatus(EventQueryStatus.valueOf(row.getString("status")));
      eventQuery.setType(EventQueryType.DEFAULT);
      Integer type = row.getInt("type");
      if (type == 1) {
        eventQuery.setType(EventQueryType.DAY_OF_WEEK);
      }
      eventQuery.setWeight(row.getInt("weight"));

      eventQuery.setDescription(row.getString("description"));
      results.add(eventQuery);
    }
    return results;
  }

  public void changeEventQueryStatus(EventQuery eventQuery) throws Exception {
    String sql =
        "UPDATE " + keyspace + ".rules set status = ? where event_name = ? AND rule_name = ?";
    List<Object> params = new ArrayList<Object>();
    params.add(eventQuery.getStatus().toString());
    params.add(eventQuery.getEventName());
    params.add(eventQuery.getRuleName());
    execute(sql, params);
  }

  public void insertEventQuery(EventQuery eventQuery) throws Exception {
    String sql =
        "INSERT INTO "
            + keyspace
            + ".rules (event_name, rule_name, data, fields, filters, aggregate_field, having, small_bucket, big_bucket, conditions, description, status, type, weight) VALUES (?, ?, ?, ?, ?, ?, ? , ?, ? , ?, ?, ?, ?, ?);";
    List<Object> params = new ArrayList<Object>();
    params.add(eventQuery.getEventName());
    params.add(eventQuery.getRuleName());
    params.add(eventQuery.getData());
    params.add(eventQuery.getFields());
    params.add(eventQuery.getFilters());
    params.add(eventQuery.getAggregateField());
    params.add(eventQuery.getHaving());
    params.add(eventQuery.getSmallBucket());
    params.add(eventQuery.getBigBucket());
    params.add(eventQuery.getConditions());
    params.add(eventQuery.getDescription());
    params.add(eventQuery.getStatus().toString());
    if (eventQuery.getType() == null) {
      params.add(0);
    } else {
      switch (eventQuery.getType()) {
        case DEFAULT:
          params.add(0);
          break;
        case DAY_OF_WEEK:
          params.add(1);
          break;
        default:
          break;
      }
    }
    params.add(eventQuery.getWeight());
    execute(sql, params);
  }

  public void deleteEventQuery(EventQuery eventQuery) throws Exception {
    String sql = "DELETE FROM " + keyspace + ".rules WHERE event_name = ? and rule_name = ?";
    List<Object> params = new ArrayList<Object>();
    params.add(eventQuery.getEventName());
    params.add(eventQuery.getRuleName());
    execute(sql, params);
  }

  /**
   * Get rule exception
   * 
   * @param eventQuery
   * @param actionStr
   * @return
   * @throws Exception
   */
  public List<EventQueryException> getEventQueyExceptionNotExpired(EventQuery eventQuery,
      String actionStr) throws Exception {
    String sql =
        "SELECT * FROM "
            + keyspace
            + ".condition_exception where event_name = ? and rule_name = ? and action = ? and expired_date >= dateOf(now())"
            + " ALLOW FILTERING;";
    List<Object> params = new ArrayList<Object>();
    params.add(eventQuery.getEventName());
    params.add(eventQuery.getRuleName());
    params.add(actionStr);
    ResultSet rows = execute(sql, params);
    List<EventQueryException> results = null;
    EventQueryException eventQueryException = null;
    for (Row row : rows) {
      if (results == null) {
        results = new ArrayList<EventQueryException>();
      }
      String eventName = row.getString("event_name");
      String ruleName = row.getString("rule_name");
      ExptionAction action =
          EventQueryException.ExptionAction.getContentType(row.getString("action"));
      Date expiredDate = row.getDate("expired_date");
      Map<String, Object> conditionFilter =
          JsonHelper.toMap(new JSONObject(row.getString("condition_filter")));
      eventQueryException =
          new EventQueryException(eventName, ruleName, action, expiredDate, conditionFilter);
      results.add(eventQueryException);
    }
    return results;
  }

  public void insertEventQueryException(EventQueryException eventQueryException) throws Exception {
    String sql =
        "INSERT INTO "
            + keyspace
            + ".condition_exception (id, event_name, rule_name, action, expired_date, condition_filter) VALUES (uuid(), ?, ?, ?, ?, ?);";
    List<Object> params = new ArrayList<Object>();
    params.add(eventQueryException.getEventName());
    params.add(eventQueryException.getRuleName());
    params.add(eventQueryException.getAction().toString());
    params.add(eventQueryException.getExpiredDate());
    params.add(eventQueryException.getConditionFilter().toString());
    execute(sql, params);
  }

  public void deleteEventQueryException(UUID id) throws Exception {
    String sql = "DELETE FROM " + keyspace + ".condition_exception WHERE id = ?";
    List<Object> params = new ArrayList<Object>();
    params.add(id);
    execute(sql, params);
  }

  /**
   * Insert result computation
   * 
   * @param eventName
   * @param rule
   * @param time
   * @param hashKey
   * @param result
   * @throws Exception
   */
  public void insertResultComputation(String eventName, String rule, long time, String hashKey,
      String result) throws Exception {
    String sql =
        "INSERT INTO "
            + keyspace
            + ".result_computation (event_name, rule_name, time, hashkey, result) VALUES (?,?,?,?,?);";
    List<Object> params = new ArrayList<Object>();
    params.add(eventName);
    params.add(rule);
    params.add(time);
    params.add(hashKey);
    params.add(result);
    execute(sql, params);
  }

  /**
   * Insert result into db
   * 
   * @param eventName
   * @param hashKey
   * @param result
   * @param filterResult
   * @throws Exception
   */
  public void insertResults(String eventName, String hashKey, List<String> result,
      List<String> filterResult) throws Exception {
    String sql =
        "INSERT INTO " + keyspace
            + ".results (event_name, hashkey, result, filtered_result) VALUES (?, ?, ?, ?);";
    List<Object> params = new ArrayList<Object>();
    params.add(eventName);
    params.add(hashKey);
    params.add(result);
    params.add(filterResult);
    execute(sql, params);
  }

  /**
   * Insert result into db
   * 
   * @param eventResult
   * @throws Exception
   */
  public void insertResults(EventResult eventResult) throws Exception {
    String sql =
        "INSERT INTO " + keyspace
            + ".results (event_name, hashkey, result, filtered_result) VALUES (?, ?, ?, ?);";
    List<Object> params = new ArrayList<Object>();
    params.add(eventResult.getEventName());
    params.add(eventResult.getHashKey());
    if (eventResult.getResult() != null && eventResult.getResult().size() > 0) {
      params.add(eventResult.getResult());
    } else {
      params.add(null);
    }
    if (eventResult.getResult() != null && eventResult.getResult().size() > 0) {
      params.add(eventResult.getFilteredResult());
    } else {
      params.add(null);
    }
    execute(sql, params);
  }

  /**
   * Update result
   * 
   * @param eventResult
   * @throws Exception
   */
  public void updateResults(EventResult eventResult) throws Exception {
    String sql =
        "UPDATE "
            + keyspace
            + ".results SET result = result + ?, filtered_result = filtered_result + ? where event_name = ? and hashkey = ?";
    List<Object> params = new ArrayList<Object>();
    List<String> result = new ArrayList<String>();
    if (eventResult.getResult() != null && eventResult.getResult().size() > 0) {
      params.add(eventResult.getResult());
    } else {
      params.add(result);
    }
    if (eventResult.getFilteredResult() != null && eventResult.getFilteredResult().size() > 0) {
      params.add(eventResult.getFilteredResult());
    } else {
      params.add(result);
    }
    params.add(eventResult.getEventName());
    params.add(eventResult.getHashKey());
    execute(sql, params);
  }

  /**
   * Get event result
   * 
   * @param eventName
   * @param hashkey
   * @return
   * @throws Exception
   */
  public List<EventResult> getEventResult(String eventName, String hashkey) throws Exception {
    String sql = "SELECT * FROM " + keyspace + ".results WHERE event_name = ? and hashkey = ?;";
    List<Object> params = new ArrayList<Object>();
    params.add(eventName);
    params.add(hashkey);
    ResultSet rows = execute(sql, params);
    List<EventResult> results = null;
    EventResult eventResult = null;
    for (Row row : rows) {
      if (results == null) {
        results = new ArrayList<EventResult>();
      }
      eventResult =
          new EventResult(row.getString("event_name"), row.getString("hashkey"), row.getList(
              "result", String.class).toString(), row.getList("filtered_result", String.class)
              .toString());
      results.add(eventResult);
    }
    return results;
  }

  /**
   * Execute query
   * 
   * @param sql
   * @param listParams
   * @return
   */
  public ResultSet execute(String sql, List<Object> listParams) throws Exception {
    ResultSet res = null;
    if (sql == Constants.EMPTY_STRING) {
      return null;
    }
    String cqlStatement = sql;
    try {
      if (listParams != null && listParams.size() > 0) {
        PreparedStatement statement = null;
        if (!listPreparedStatements.keySet().contains(cqlStatement)) {
          statement = session.prepare(cqlStatement);
          listPreparedStatements.put(cqlStatement, statement);
        } else {
          statement = listPreparedStatements.get(cqlStatement);
        }
        BoundStatement boundStatement = new BoundStatement(statement);
        res = session.execute(boundStatement.bind(listParams.toArray()));
      } else {
        res = session.execute(cqlStatement);
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw ex;
    }
    return res;
  }

  public List<EventQuery> getEventQueryFromDB(List<String> lstEventName) throws Exception {
    StringBuilder sql = new StringBuilder("SELECT * FROM " + keyspace + ".rules");
    List<Object> params = new ArrayList<Object>();
    if (lstEventName != null && !lstEventName.isEmpty()) {
      sql.append(" WHERE event_name in (' '");
      for (String child : lstEventName) {
        sql.append(", ?");
        params.add(child);
      }
      sql.append(") ");
    }
    ResultSet rows = execute(sql.toString(), params);
    List<EventQuery> results = null;
    EventQuery eventQuery = null;
    for (Row row : rows) {
      if (results == null) {
        results = new ArrayList<EventQuery>();
      }
      eventQuery = new EventQuery();
      eventQuery.setEventName(row.getString("event_name"));
      eventQuery.setData(row.getString("data"));
      eventQuery.setFields(row.getString("fields"));
      eventQuery.setFilters(row.getString("filters"));
      eventQuery.setAggregateField(row.getString("aggregate_field"));
      eventQuery.setSmallBucket(row.getString("small_bucket"));
      eventQuery.setBigBucket(row.getString("big_bucket"));
      eventQuery.setConditions(row.getString("conditions"));
      eventQuery.setRuleName(row.getString("rule_name"));
      eventQuery.setHaving(row.getString("having"));
      eventQuery.setDescription(row.getString("description"));
      eventQuery.setStatus(EventQueryStatus.valueOf(row.getString("status")));
      results.add(eventQuery);
    }
    return results;
  }

  /**
   * Get result of continue query
   * @param eventName
   * @param ruleName
   * @param startTime
   * @param endTime
   * @param limit
   * @return
   * @throws Exception
   */
  public List<ResultComputation> getResultComputation(String eventName, String ruleName,
      long startTime, long endTime, int limit) throws Exception {
    String sql =
        "SELECT * FROM "
            + keyspace
            + ".result_computation where event_name = ? and rule_name = ? and time >= ? and time <=?  limit ?";
    List<Object> params = new ArrayList<Object>();
    params.add(eventName);
    params.add(ruleName);
    params.add(startTime);
    params.add(endTime);
    params.add(limit);
    ResultSet rows = execute(sql, params);
    List<ResultComputation> results = null;
    ResultComputation tmp = null;
    for (Row row : rows) {
      if (results == null) {
        results = new ArrayList<ResultComputation>();
      }
      tmp =
          new ResultComputation(eventName, ruleName,
              row.getLong("time"), row.getString("hashkey"), row.getString("result"));
      results.add(tmp);
    }
    return results;
  }

}

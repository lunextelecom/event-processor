package com.lunex.eventprocessor.core.dataaccess;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import com.lunex.eventprocessor.core.EventResult;
import com.lunex.eventprocessor.core.utils.Constants;
import com.lunex.eventprocessor.core.utils.StringUtils;

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
  public static CassandraRepository getInstance() throws Exception {
    if (instance == null) {
      Properties prop = new Properties();
      InputStream inputStream = new FileInputStream("src/main/resources/app.properties");
      prop.load(inputStream);
      String host = prop.getProperty("cassandra.host");
      String keyspace = prop.getProperty("cassandra.keyspace");
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
    KeyspaceMetadata keyspaceMetadata = metadata.getKeyspace(instance.keyspace);
    if (keyspaceMetadata == null) {
      String sql =
          "CREATE KEYSPACE " + instance.keyspace
              + " WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'dc1' : 2 };";
      instance.session.execute(sql);
      metadata = instance.cluster.getMetadata();
      keyspaceMetadata = metadata.getKeyspace(instance.keyspace);
    }
    if (metadata != null) {

      keyspaceMetadata = metadata.getKeyspace(instance.keyspace);
      if (keyspaceMetadata == null) {
        throw new UnsupportedOperationException("Can't find keyspace :" + instance.keyspace);
      }
      if (keyspaceMetadata.getTable("rules") == null) {
        String sql =
            "CREATE TABLE "
                + instance.keyspace
                + ".rules (id uuid, event_name text, rule_name text, data text,  fields text, filters text, aggregate_field text, having text, time_series text, PRIMARY KEY (id, event_name, rule_name))";
        instance.session.execute(sql);
      }
      if (keyspaceMetadata.getTable("events") == null) {
        String sql =
            "CREATE TABLE "
                + instance.keyspace
                + ".events (event_name text, time bigint, event text, PRIMARY KEY (event_name, time))";
        instance.session.execute(sql);
      }
      if (keyspaceMetadata.getTable("results") == null) {
        String sql =
            "CREATE TABLE "
                + instance.keyspace
                + ".results (event_name text, hashkey text, result text, filtered_result text, PRIMARY KEY (event_name, hashkey))";
        instance.session.execute(sql);
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
            + ".events (event_name, time, event, hashkey) VALUES (?, ?, ?, ?);";
    List<Object> params = new ArrayList<Object>();
    params.add(event.getEvtName());
    params.add(event.getTime());
    params.add(event.getPayLoadStr());
    params.add((event.getPayLoadStr() != null && !Constants.EMPTY_STRING.equals(event
        .getPayLoadStr())) ? StringUtils.md5Java(event.getPayLoadStr()) : "");
    execute(sql, params);
  }

  /**
   * Get event query (rule) from DB
   * 
   * @param id
   * @param eventName
   * @param ruleName
   * @return
   * @throws Exception
   */
  public List<EventQuery> getEventQueryFromDB(int id, String eventName, String ruleName)
      throws Exception {
    String sql = "SELECT * FROM " + keyspace + ".rules";
    List<Object> params = new ArrayList<Object>();
    if (id != -1 || !Constants.EMPTY_STRING.equals(eventName)
        || !Constants.EMPTY_STRING.equals(ruleName)) {
      sql += " WHERE 1 = 1 ";
    }
    if (id != -1) {
      sql += " AND id = ? ";
      params.add(id);
    }
    if (!Constants.EMPTY_STRING.equals(eventName)) {
      sql += " AND event_name = ? ";
      params.add(eventName);
    }
    if (!Constants.EMPTY_STRING.equals(eventName)) {
      sql += " AND rule_name = ? ";
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
      eventQuery.setTimeSeries(row.getString("time_series"));
      eventQuery.setConditions(row.getString("conditions"));
      results.add(eventQuery);
    }
    return results;
  }

  public void insertEventQuery() {
    // TODO
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
  public void insertResults(String eventName, String hashKey, String result, String filterResult)
      throws Exception {
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
   * Get event result
   * 
   * @param eventName
   * @param hashkey
   * @return
   * @throws Exception
   */
  public List<EventResult> getEventResult(String eventName, String hashkey) throws Exception {
    String sql = "SELET * FROM " + keyspace + ".results WHERE event_name = ? and hashkey = ?;";
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
          new EventResult(row.getString("event_name"), row.getString("hashkey"),
              row.getString("result"), row.getString("filtered_result"));
      results.add(eventResult);
    }
    return results;
  }

  /**
   * Execute non query
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

}

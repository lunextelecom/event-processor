package com.lunex.eventprocessor.core.dataaccess;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
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
import com.lunex.eventprocessor.core.EventProperty;
import com.lunex.eventprocessor.core.EventQuery;
import com.lunex.eventprocessor.core.utils.Constants;

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
      if (keyspaceMetadata.getTable("rule") == null) {
        String sql =
            "CREATE TABLE "
                + instance.keyspace
                + ".rule (id uuid, event_name text, rule_name text, data text,  fields text, filters text, aggregate_field text, having text, time_series text, PRIMARY KEY (id, event_name, rule_name))";
        instance.session.execute(sql);
      }
      if (keyspaceMetadata.getTable("event") == null) {
        String sql =
            "CREATE TABLE "
                + instance.keyspace
                + ".time (time bigint, event_name text, event text, PRIMARY KEY (time, event_name))";
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
      logger.error(ex.getMessage());
      return false;
    }
  }

  public void insertEventToDB(Event event) throws Exception {
    String sql = "INSERT INTO event (time, event_name, event) VALUES (?, ?, ?);";
    List<Object> params = new ArrayList<Object>();
    params.add(event.getTime());
    params.add(event.getEvtName());
    params.add(event.getEvent());
    execute(sql, params);
  }

  public List<EventQuery> getEventQueryFromDB(int id, String eventName, String ruleName)
      throws Exception {
    String sql = "SELECT * FROM " + keyspace + ".rule";
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
      results.add(eventQuery);
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
      logger.error(ex.getMessage());
      throw ex;
    }
    return res;
  }

}

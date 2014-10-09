package com.lunex.eventprocessor.core.dataaccess;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kairosdb.client.HttpClient;
import org.kairosdb.client.builder.Aggregator;
import org.kairosdb.client.builder.Metric;
import org.kairosdb.client.builder.MetricBuilder;
import org.kairosdb.client.builder.QueryBuilder;
import org.kairosdb.client.builder.QueryMetric;
import org.kairosdb.client.builder.TimeUnit;
import org.kairosdb.client.response.GetResponse;
import org.kairosdb.client.response.QueryResponse;


public class KairosDBClient {

  private String kairosDbUrl;

  public KairosDBClient(String url) {
    kairosDbUrl = url;
  }

  /**
   * Send metric to kairosDB
   * 
   * @param metricName
   * @param timestamp
   * @param value
   * @param tags
   * @throws URISyntaxException
   * @throws IOException
   */
  public void sendMetric(String metricName, long timestamp, Object value, Map<String, String> tags)
      throws URISyntaxException, IOException {
    MetricBuilder builder = MetricBuilder.getInstance();
    Metric metric = builder.addMetric(metricName);
    if (tags != null && !tags.isEmpty()) {
      Iterator<String> keys = tags.keySet().iterator();
      String key = null;
      while (keys.hasNext()) {
        key = keys.next();
        metric.addTag(key, tags.get(key));
      }
    }
    metric.addDataPoint(timestamp, value);
    HttpClient client = new HttpClient(kairosDbUrl);
    client.pushMetrics(builder);
    client.shutdown();
  }

  /**
   * Send map metric to kairosDB
   * 
   * @param metricName
   * @param points
   * @param tags
   * @throws URISyntaxException
   * @throws IOException
   */
  public void sendMetric(String metricName, Map<Long, Object> points, Map<String, String> tags)
      throws URISyntaxException, IOException {
    if (points == null || points.isEmpty()) {
      return;
    }
    MetricBuilder builder = MetricBuilder.getInstance();
    Metric metric = builder.addMetric(metricName);
    if (tags != null && !tags.isEmpty()) {
      Iterator<String> keys = tags.keySet().iterator();
      String key = null;
      while (keys.hasNext()) {
        key = keys.next();
        metric.addTag(key, tags.get(key));
      }
    }
    Iterator<Long> keys = points.keySet().iterator();
    while (keys.hasNext()) {
      long key = keys.next();
      metric.addDataPoint(key, points.get(key));
    }
    HttpClient client = new HttpClient(kairosDbUrl);
    client.pushMetrics(builder);
    client.shutdown();
  }

  /**
   * Query get metric name
   * 
   * @return
   * @throws IOException
   */
  public List<String> queryMetricNames() throws IOException {
    HttpClient client = new HttpClient(kairosDbUrl);
    GetResponse response = client.getTagNames();
    List<String> metricNames = response.getResults();
    client.shutdown();
    return metricNames;
  }

  /**
   * Query get data point
   * 
   * @param metricName
   * @param startDuration
   * @param startTimeunit
   * @param endDuration
   * @param endTimeunit
   * @param listAggregator
   * @return
   * @throws URISyntaxException
   * @throws IOException
   */
  public QueryResponse queryDataPoints(String metricName, int startDuration,
      TimeUnit startTimeunit, int endDuration, TimeUnit endTimeunit, List<Aggregator> listAggregator)
      throws URISyntaxException, IOException {
    QueryBuilder builder = QueryBuilder.getInstance();
    if (startDuration != -1) {
      builder = builder.setStart(startDuration, startTimeunit);
    }
    if (endDuration != -1) {
      builder = builder.setEnd(endDuration, endTimeunit);
    }
    QueryMetric queryMetric = builder.addMetric(metricName);

    if (listAggregator != null && listAggregator.size() > 0) {
      for (int i = 0; i < listAggregator.size(); i++) {
        queryMetric.addAggregator(listAggregator.get(i));
      }
    }
    HttpClient client = new HttpClient(kairosDbUrl);
    QueryResponse response = client.query(builder);
    client.shutdown();
    return response;
  }

  public static void main(String[] args) {
    KairosDBClient client = new KairosDBClient("http://10.9.9.61:8080");
    try {
      Map<String, String> tags = (new HashMap<String, String>());
      tags.put("name", "metric1");
      client.sendMetric("metric1", System.currentTimeMillis(), 200, tags);
      // System.out.println(client.queryMetricNames().toString());
      QueryResponse response =
          client.queryDataPoints("metric1", 1, TimeUnit.MONTHS, -1, TimeUnit.MONTHS, null);
      System.out.println(response.getQueries().get(0).getResults().get(0).getDataPoints());
    } catch (URISyntaxException e) {
    } catch (IOException e) {
    }
  }
}

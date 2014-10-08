package com.lunex.eventprocessor.core.dataaccess;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

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

  public void sendMetric(String metricName, long timestamp, Object value,
      List<Map<String, String>> tags) throws URISyntaxException, IOException {
    MetricBuilder builder = MetricBuilder.getInstance();
    Metric metric = builder.addMetric(metricName);
    for (int i = 0; i < tags.size(); i++) {
//      metric.addTag(tags.get(i)., value)
    }
    metric.addTag("host", "server1").addTag("customer", "Acme").addDataPoint(timestamp, value);
    HttpClient client = new HttpClient("http://10.9.9.61:8080");
    client.pushMetrics(builder);
    client.shutdown();
  }

  public List<String> queryMetricNames() throws IOException {
    HttpClient client = new HttpClient(kairosDbUrl);
    GetResponse response = client.getTagNames();
    List<String> metricNames = response.getResults();
    client.shutdown();
    return metricNames;
  }

  public QueryResponse queryDataPoints(String metricName, int startDuration,
      TimeUnit startTimeunit, int endDuration, TimeUnit endTimeunit, List<Aggregator> listAggregator)
      throws URISyntaxException, IOException {
    QueryBuilder builder = QueryBuilder.getInstance();
    QueryMetric queryMetric =
        builder.setStart(startDuration, startTimeunit).setEnd(endDuration, endTimeunit)
            .addMetric(metricName);

    for (int i = 0; i < listAggregator.size(); i++) {
      queryMetric.addAggregator(listAggregator.get(i));
    }
    HttpClient client = new HttpClient(kairosDbUrl);
    QueryResponse response = client.query(builder);
    client.shutdown();
    return response;
  }

  public static void main(String[] args) {
    KairosDBClient client = new KairosDBClient("http://10.9.9.61:8080");
    try {
      client.sendMetric("metric1", System.currentTimeMillis(), 100, null);
      System.out.println(client.queryMetricNames().toString());
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}

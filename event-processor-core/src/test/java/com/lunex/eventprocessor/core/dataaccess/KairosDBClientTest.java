package com.lunex.eventprocessor.core.dataaccess;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.kairosdb.client.builder.TimeUnit;
import org.kairosdb.client.response.QueryResponse;

public class KairosDBClientTest {

  @Test
  public void testSendAndQueryDataPoints() {
    KairosDBClient client = new KairosDBClient("http://10.9.9.61:8080");
    Map<String, String> tags = (new HashMap<String, String>());
    tags.put("tagTest", "tagTest");
    try {
      client.sendMetric("testMetric", System.currentTimeMillis(), 200, tags);
      QueryResponse response =
          client.queryDataPoints("testMetric", 1, TimeUnit.MONTHS, -1, TimeUnit.MONTHS, null);
      Object value = response.getQueries().get(0).getResults().get(0).getDataPoints().get(0).getValue();
      if (response != null && value.toString().equals("200")) {
        assertEquals(1, 1);
      } else {
        assertEquals(1, 0);
      }
    } catch (URISyntaxException | IOException e) {
      assertEquals(1, 0);
    }
  }

}

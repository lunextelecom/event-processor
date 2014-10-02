package com.lunex.eventprocessor.input.kafka;

import com.lunex.eventprocessor.input.utils.StringUtils;

import kafka.producer.Partitioner;
import kafka.utils.VerifiableProperties;

public class Md5Partitioner implements Partitioner<Object> {

  public Md5Partitioner(VerifiableProperties props) {}

  public int partition(Object key, int countPartitions) {
    String md5 = StringUtils.md5Java((String) key);
    String str = md5.replaceAll("\\D+", "");
    return (Integer.valueOf(str) % countPartitions);
  }


}

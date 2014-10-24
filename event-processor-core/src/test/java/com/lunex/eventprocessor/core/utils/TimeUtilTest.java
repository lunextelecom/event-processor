package com.lunex.eventprocessor.core.utils;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

public class TimeUtilTest {

  @Test
  public void testConvertStringToDate() {
    Date date = TimeUtil.convertStringToDate("23-10-2014 20:49:18 PDT", "dd-MM-yyyy HH:mm:ss z");
    assertEquals(1414122558000L, date.getTime());
  }

  @Test
  public void testConvertDateToGMT_7() {
//    fail("Not yet implemented");
  }

  @Test
  public void testConvertDateToUnixTime() {
//    fail("Not yet implemented");
  }

  @Test
  public void testConvertDateToString() {
//    fail("Not yet implemented");
  }

  @Test
  public void testGetMonday() {
//    fail("Not yet implemented");
  }

}

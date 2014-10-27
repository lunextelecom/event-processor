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
    Date date = new Date(25200000);
    long ms = TimeUtil.convertDateMilisecond(date);
    assertEquals(25200000, ms);
  }

  @Test
  public void testGetMonday() {
    Date date = new Date(25200000);
    Date monday = TimeUtil.getMonday(date);
    assertEquals("Mon Dec 29 14:00:00 ICT 1969", monday.toString());
  }

  @Test
  public void getBeginTime() {
    Date date = new Date();
    long time = date.getTime();
    long startTime = TimeUtil.getBeginTime(time);
    long endtime = TimeUtil.getEndTime(time);
    Date startDate = new Date(startTime);
    Date endDate = new Date(endtime);
    assertEquals(TimeUtil.convertDateToString(date, "dd/MM/yyyy") + " 00:00:00",
        TimeUtil.convertDateToString(startDate, "dd/MM/yyyy HH:mm:ss"));
    assertEquals(TimeUtil.convertDateToString(date, "dd/MM/yyyy") + " 23:59:59",
        TimeUtil.convertDateToString(endDate, "dd/MM/yyyy HH:mm:ss"));
  }
}

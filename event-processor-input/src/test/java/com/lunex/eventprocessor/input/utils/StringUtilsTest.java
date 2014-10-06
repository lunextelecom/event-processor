package com.lunex.eventprocessor.input.utils;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StringUtilsTest {

  @BeforeClass
  public static void beforeClass() {
    System.out.println("before Class");
  }

  @Test
  public void testIsJSONValid() {
    System.out.println("testIsJSONValid");
    assertEquals(true, StringUtils.isJSONValid("{\"amount\": 10}"));
    assertEquals(false, StringUtils.isJSONValid("{\"amount: 10}"));
  }

  @Test
  public void testMd5Java() {
    assertEquals("900150983cd24fb0d6963f7d28e17f72", StringUtils.md5Java("abc"));
  }

  @AfterClass
  public static void afterClass() {
    System.out.println("after Class");
  }
}

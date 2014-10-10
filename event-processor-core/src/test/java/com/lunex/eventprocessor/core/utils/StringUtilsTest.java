package com.lunex.eventprocessor.core.utils;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class StringUtilsTest {

  @Test
  public void testIsJSONValid() {
    System.out.println("testIsJSONValid");
    assertEquals(true, StringUtils.isJSONValid("{\"amount\": 10}"));
    assertEquals(false, StringUtils.isJSONValid("{\"amount: 10}"));
  }

  @Test
  public void testMd5Java() throws UnsupportedEncodingException, NoSuchAlgorithmException {
    assertEquals("900150983cd24fb0d6963f7d28e17f72", StringUtils.md5Java("abc"));
  }

}

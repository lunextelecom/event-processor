package com.lunex.eventprocessor.handler.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
	public static Date convertStringToDate(String input, String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		String dateInString = input;
		try {
			Date date = formatter.parse(dateInString);
			return date;
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static long convertDateToGMT_7(Date date) {
		return Long.valueOf(date.getTime());
	}

	public static long convertDateToUnixTime(Date date) {
		return Long.valueOf(date.getTime() / 1000L);
	}

	public static String convertDateToString(Date date, String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(date);
	}

	public static Date getMonday(Date date) {
		Calendar c = Calendar.getInstance();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		c.setTime(date);
		int today = c.get(Calendar.DAY_OF_WEEK);
		c.add(Calendar.DAY_OF_WEEK, -today + Calendar.MONDAY);
		return c.getTime();
	}
}

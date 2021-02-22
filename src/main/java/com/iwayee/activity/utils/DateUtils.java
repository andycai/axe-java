package com.iwayee.activity.utils;

import java.util.Date;

public class DateUtils {
  public static int MILLISECOND = 1;
  public static int SECOND;
  public static int MINUTE;
  public static int HOUR;
  public static int DAY;

  public DateUtils() {
  }

  static {
    SECOND = MILLISECOND * 1000;
    MINUTE = 60 * SECOND;
    HOUR = 60 * MINUTE;
    DAY = 24 * HOUR;
  }

  public static long getDiffMinutes(Date one, Date two) {
    long minutes = 0;
    try {
      long time1 = one.getTime();
      long time2 = two.getTime();
      long diff = time2 - time1;
      minutes = diff / MINUTE;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return minutes;
  }

  public static long getDiffHours(Date one, Date two) {
    long hours = 0;
    try {
      long time1 = one.getTime();
      long time2 = two.getTime();
      long diff = time2 - time1;
      hours = diff / HOUR;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return hours;
  }
}
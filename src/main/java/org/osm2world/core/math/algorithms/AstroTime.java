package org.osm2world.core.math.algorithms;

import static java.lang.Math.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Astronomical time calculation functions
 */
public class AstroTime {
  /**
   * Calculate julian day number from a date
   * @param date
   * @return
   */
  public static double julianDayNumber(LocalDate date) {
    int year = date.getYear();
    int month = date.getMonth().getValue();
    int day = date.getDayOfMonth();

    if (month <= 2) {
      year -= 1;
      month += 12;
    }

    double a = floor(year / 100.0);
    double b = 2 - a + floor(a / 4);

    double jd = floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) +
                day + b - 1524.5;

    return jd;
  }

  /*
   * minutesDay
   * @param time
   * @return number of fractional minutes since start of day
   */
  public static double minutesDay(LocalTime time) {
    return time.toSecondOfDay() / 60.0;
  }

  /*
   * julianCenturyTime
   * @param time
   * @return  number of fractional julian centuries since the julian epoch
   */
  public static double julianCenturyTime(ZonedDateTime time) {
    double jdaynum = julianDayNumber(time.toLocalDate());
    double dayMinute = minutesDay(time.toLocalTime());
    double tz = time.getOffset().getTotalSeconds();
    double jday = jdaynum + dayMinute / 1440.0 - (tz * 60);

    return (jday - 2451545.0) / 36525.0;
  }
}

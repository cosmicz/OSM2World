package org.osm2world.core.math.algorithms;

import static java.lang.Math.*;

import java.util.Calendar;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Sun position calculation. Adapted from
 * https://www.esrl.noaa.gov/gmd/grad/solcalc/
 */
public class SunPosition {
  public final double lat, lon;
  public final ZoneOffset zone;

  /**
   *
   */
  public SunPosition(double lat, double lon, ZoneOffset zone) {
    this.lat = lat;
    this.lon = lon;
    this.zone = zone;
  }

  public void sunrise() {}

  public void sunset() {}

  /**
   * Calculate julian day number from a date
   *
   * @param date
   * @return
   */
  private static double julianDayNumber(LocalDate date) {
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

  private static double minutesDay(LocalTime time) {
    return 60.0 * time.getHour() + time.getMinute() + time.getSecond() / 60.0;
  }

  private static double julianCenturyTime(ZonedDateTime time) {
    double jdaynum = julianDayNumber(time.toLocalDate());
    double dayMinute = minutesDay(time.toLocalTime());
    double tz = time.getOffset().getTotalSeconds();
    double jday = jdaynum + dayMinute / 1440.0 - (tz * 60);

    return (jday - 2451545.0) / 36525.0;
  }

  /**
   * SunPositionAt
   */
  class SunPositionAt {
    public final double lat, lon;
    public final ZonedDateTime dateTime;

    private boolean computed = false;
    private double azimuth;
    private double zenith;

    public SunPositionAt(double lat, double lon, ZonedDateTime dateTime) {
      this.lat = lat;
      this.lon = lon;
      this.dateTime = dateTime;
    }

    public double azimuth() {
      if (!computed) {
        this.calcAzEl();
        computed = true;
      }
      return azimuth;
    }

    public double zenith() {
      if (!computed) {
        this.calcAzEl();
        computed = true;
      }
      return zenith;
    }

    public double elevation() {
      if (!computed) {
        this.calcAzEl();
        computed = true;
      }
      return 90 - zenith;
    }

    private void calcAzEl() {
      double jt = julianCenturyTime(dateTime);
      double eqTime = equationOfTime(jt);
      double theta = sunDeclination(jt);
      double solarTimeFix =
          eqTime + 4.0 * this.lon - 3600 * zone.getTotalSeconds();
      // double earthRadVec = sunRadVector(jt);
      double trueSolarTime =
          SunPosition.minutesDay(dateTime.toLocalTime()) + solarTimeFix;
      while (trueSolarTime > 1440) {
        trueSolarTime -= 1440;
      }
      double hourAngle = trueSolarTime / 4.0 - 180.0;
      if (hourAngle < -180) {
        hourAngle += 360.0;
      }
      double haRad = degToRad(hourAngle);
      double csz = sin(degToRad(lat)) * sin(degToRad(theta)) +
                   cos(degToRad(lat)) * cos(degToRad(theta)) * cos(haRad);
      if (csz > 1.0) {
        csz = 1.0;
      } else if (csz < -1.0) {
        csz = -1.0;
      }

      zenith = radToDeg(acos(csz));
      double azDenom = cos(degToRad(lat)) * sin(degToRad(zenith));
      if (abs(azDenom) > 0.001) {
        double azRad = (sin(degToRad(lat)) * cos(degToRad(zenith)) -
                        sin(degToRad(theta))) /
                       azDenom;
        if (abs(azRad) > 1.0) {
          if (azRad < 0) {
            azRad = -1.0;
          } else {
            azRad = 1.0;
          }
        }
        azimuth = 180.0 - radToDeg(acos(azRad));
        if (hourAngle > 0.0) {
          azimuth = -azimuth;
        }
      } else {
        if (this.lat > 0.0) {
          azimuth = 180.0;
        } else {
          azimuth = 0.0;
        }
      }
      if (azimuth < 0.0) {
        azimuth += 360.0;
      }
      double exoatmElevation = 90.0 - zenith;

      // atmospheric refraction correction
      double refractionCorrection;
      if (exoatmElevation > 85.0) {
        refractionCorrection = 0.0;
      } else {
        double te = tan(degToRad(exoatmElevation));
        if (exoatmElevation > 5.0) {
          refractionCorrection = 58.1 / te - 0.07 / (te * te * te) +
                                 0.000086 / (te * te * te * te * te);
        } else if (exoatmElevation > -0.575) {
          refractionCorrection =
              1735.0 +
              exoatmElevation *
                  (-518.2 +
                   exoatmElevation *
                       (103.4 +
                        exoatmElevation * (-12.79 + exoatmElevation * 0.711)));
        } else {
          refractionCorrection = -20.774 / te;
        }
        refractionCorrection = refractionCorrection / 3600.0;
      }

      zenith = zenith - refractionCorrection;
    }
  }

  public SunPositionAt at(ZonedDateTime dateTime) {
    return new SunPositionAt(lat, lon, dateTime);
  }

  private static double degToRad(double deg) { return PI * deg / 180; }

  private static double radToDeg(double rad) { return 180 * rad / PI; }

  /**
   * Solar time calculations
   */
  class SunTime {
    public final double t;
    // private static cache

    /*
     * SunTime
     * @param t
     */
    public SunTime(double t) { this.t = t; }
  }

  /**
   * @return earth orbit eccentricity
   */
  private static double eccentricityEarthOrbit(double t) {
    double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
    return e;
  }

  /**
   * @return L0 in degrees
   */
  private static double geomMeanLongSun(double t) {
    double L0 = 280.46646 + t * (36000.76983 + t * 0.0003032);
    while (L0 > 360.0) {
      L0 -= 360.0;
    }
    while (L0 < 0.0) {
      L0 += 360.0;
    }
    return L0;
  }

  /**
   * @return mean obliquity of ecliptic in degrees
   */
  private static double meanObliquityOfEcliptic(double t) {
    double seconds = 21.448 - t * (46.815 + t * (0.00059 - t * 0.001813));
    double e0 = 23.0 + (26.0 + seconds / 60.0) / 60.0;
    return e0;
  }

  /**
   * @return obliquity correction in degrees
   */
  private static double obliquityCorrection(double t) {
    double e0 = meanObliquityOfEcliptic(t);
    double omega = 125.04 - 1934.136 * t;
    double e = e0 + 0.00256 * cos(degToRad(omega));

    return e;
  }

  /**
   * @return M in degrees
   */
  private static double geomMeanAnomalySun(double t) {
    double M = 357.52911 + t * (35999.05029 - 0.0001537 * t);
    return M; // in degrees
  }

  private static double equationOfTime(double t) {
    double epsilon = obliquityCorrection(t);
    double l0 = geomMeanLongSun(t);
    double e = eccentricityEarthOrbit(t);
    double m = geomMeanAnomalySun(t);

    double y = tan(degToRad(epsilon) / 2);
    y *= y;

    double sin2l0 = sin(2 * degToRad(l0));
    double cos2l0 = cos(2 * degToRad(l0));
    double sin4l0 = sin(4 * degToRad(l0));
    double sinm = sin(degToRad(m));
    double sin2m = sin(2 * degToRad(m));

    double eTime = (y * sin2l0) - (2 * e * sinm) + (4 * e * y * sinm * cos2l0) -
                   (0.5 * y * y * sin4l0) - (1.25 * e * e * sin2m);

    return radToDeg(eTime) * 4;
  }

  /**
   * @return C in degrees
   */
  private static double sunEqOfCenter(double t) {
    double m = geomMeanAnomalySun(t);
    double mrad = degToRad(m);
    double sinm = Math.sin(mrad);
    double sin2m = Math.sin(mrad + mrad);
    double sin3m = Math.sin(mrad + mrad + mrad);
    double C = sinm * (1.914602 - t * (0.004817 + 0.000014 * t)) +
               sin2m * (0.019993 - 0.000101 * t) + sin3m * 0.000289;
    return C;
  }

  /**
   * @return O in degrees
   */
  private static double sunTrueLong(double t) {
    double l0 = geomMeanLongSun(t);
    double c = sunEqOfCenter(t);
    double O = l0 + c;
    return O;
  }

  /**
   * @return apparent sun time in degrees
   */
  private static double sunApparentLong(double t) {
    double o = sunTrueLong(t);
    double omega = 125.04 - 1934.136 * t;
    double lambda = o - 0.00569 - 0.00478 * Math.sin(degToRad(omega));
    return lambda;
  }

  /**
   * @return sun declination in degrees
   */
  private static double sunDeclination(double t) {
    double e = obliquityCorrection(t);
    double lambda = sunApparentLong(t);

    double sint = Math.sin(degToRad(e)) * Math.sin(degToRad(lambda));
    double theta = radToDeg(Math.asin(sint));
    return theta;
  }

  /*
   * sunTrueAnomaly
   * @param t
   * @return anomaly in degrees
   */
  private static double sunTrueAnomaly(double t) {
    double m = geomMeanAnomalySun(t);
    double c = sunEqOfCenter(t);
    double v = m + c;
    return v;
  }

  /*
   * sunRadVector
   * @param t
   * @return R in AUs
   */
  private static double sunRadVector(double t) {
    double v = sunTrueAnomaly(t);
    double e = eccentricityEarthOrbit(t);
    double R = (1.000001018 * (1 - e * e)) / (1 + e * Math.cos(degToRad(v)));
    return R;
  }
}

package org.osm2world.core.math.algorithms;

import static java.lang.Math.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Solar time-based calculations
 */
public class SunTime {
  public final double t;
  private final Map<String, Double> res = new ConcurrentHashMap<>();

  /*
   * SunTime
   * @param t astronomical time -- fractional centuries since the Julian epoch
   * at 12:00 UTC 2000-01-01
   */
  public SunTime(double t) { this.t = t; }

  /**
   * @return earth orbit eccentricity
   */
  public double eccentricityEarthOrbit() {
    return res.computeIfAbsent("eccentricityEarthOrbit", __ -> {
      double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
      return e;
    });
  }

  /**
   * @return L0 in degrees
   */
  public double geomMeanLongSun() {
    return res.computeIfAbsent("geomMeanLongSun", __ -> {
      double L0 = 280.46646 + t * (36000.76983 + t * 0.0003032);
      while (L0 > 360.0) {
        L0 -= 360.0;
      }
      while (L0 < 0.0) {
        L0 += 360.0;
      }
      return L0;
    });
  }

  /**
   * @return mean obliquity of ecliptic in degrees
   */
  public double meanObliquityOfEcliptic() {
    return res.computeIfAbsent("meanObliquityOfEcliptic", __ -> {
      double seconds = 21.448 - t * (46.815 + t * (0.00059 - t * 0.001813));
      double e0 = 23.0 + (26.0 + seconds / 60.0) / 60.0;
      return e0;
    });
  }

  /**
   * @return obliquity correction in degrees
   */
  public double obliquityCorrection() {
    return res.computeIfAbsent("obliquityCorrection", __ -> {
      double e0 = meanObliquityOfEcliptic();
      double omega = 125.04 - 1934.136 * t;
      double e = e0 + 0.00256 * cos(toRadians(omega));
      return e;
    });
  }

  /**
   * @return M in degrees
   */
  public double geomMeanAnomalySun() {
    return res.computeIfAbsent("geomMeanAnomalySun", __ -> {
      double M = 357.52911 + t * (35999.05029 - 0.0001537 * t);
      return M; // in degrees
    });
  }

  /*
   * sun equation of time
   * @return
   */
  public double equationOfTime() {
    return res.computeIfAbsent("equationOfTime", __ -> {
      double epsilon = obliquityCorrection();
      double l0 = geomMeanLongSun();
      double e = eccentricityEarthOrbit();
      double m = geomMeanAnomalySun();

      double y = tan(toRadians(epsilon) / 2);
      y *= y;

      double sin2l0 = sin(2 * toRadians(l0));
      double cos2l0 = cos(2 * toRadians(l0));
      double sin4l0 = sin(4 * toRadians(l0));
      double sinm = sin(toRadians(m));
      double sin2m = sin(2 * toRadians(m));

      double eTime = (y * sin2l0) - (2 * e * sinm) +
                     (4 * e * y * sinm * cos2l0) - (0.5 * y * y * sin4l0) -
                     (1.25 * e * e * sin2m);

      return toDegrees(eTime) * 4;
    });
  }

  /**
   * @return C in degrees
   */
  public double sunEqOfCenter() {
    return res.computeIfAbsent("sunEqOfCenter", __ -> {
      double m = geomMeanAnomalySun();
      double mrad = toRadians(m);
      double sinm = sin(mrad);
      double sin2m = sin(mrad + mrad);
      double sin3m = sin(mrad + mrad + mrad);
      double C = sinm * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                 sin2m * (0.019993 - 0.000101 * t) + sin3m * 0.000289;
      return C;
    });
  }

  /**
   * @return O in degrees
   */
  public double sunTrueLong() {
    return res.computeIfAbsent("sunTrueLong", __ -> {
      double l0 = geomMeanLongSun();
      double c = sunEqOfCenter();
      double O = l0 + c;
      return O;
    });
  }

  /**
   * @return apparent sun time in degrees
   */
  public double sunApparentLong() {
    return res.computeIfAbsent("sunApparentLong", __ -> {
      double o = sunTrueLong();
      double omega = 125.04 - 1934.136 * t;
      double lambda = o - 0.00569 - 0.00478 * sin(toRadians(omega));
      return lambda;
    });
  }

  /**
   * @return sun declination in degrees
   */
  public double sunDeclination() {
    return res.computeIfAbsent("sunDeclination", __ -> {
      double e = obliquityCorrection();
      double lambda = sunApparentLong();

      double sint = sin(toRadians(e)) * sin(toRadians(lambda));
      double theta = toDegrees(asin(sint));
      return theta;
    });
  }

  /*
   * sunTrueAnomaly
   * @param t
   * @return anomaly in degrees
   */
  public double sunTrueAnomaly() {
    return res.computeIfAbsent("sunTrueAnomaly", __ -> {
      double m = geomMeanAnomalySun();
      double c = sunEqOfCenter();
      double v = m + c;
      return v;
    });
  }

  /*
   * sunRadVector
   * @param t
   * @return R in AUs
   */
  public double sunRadVector() {
    return res.computeIfAbsent("sunRadVector", __ -> {
      double v = sunTrueAnomaly();
      double e = eccentricityEarthOrbit();
      double R = (1.000001018 * (1 - e * e)) / (1 + e * cos(toRadians(v)));
      return R;
    });
  }
}

package org.osm2world.core.math.algorithms;

import static java.lang.Math.*;

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

  /*
   *
   * @param lat
   * @param lon
   */
  public SunPosition(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;
    this.zone = null;
  }

  public void sunrise() {}

  public void sunset() {}

  /*
   *
   * @param dateTime
   * @return
   */
  public SunPositionAt at(ZonedDateTime dateTime) {
    return new SunPositionAt(lat, lon, dateTime);
  }

  public SunPositionAt at(LocalDateTime dateTime) {
    return new SunPositionAt(lat, lon, dateTime.atZone(zone));
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
        calcAzEl();
        computed = true;
      }
      return azimuth;
    }

    public double zenith() {
      if (!computed) {
        calcAzEl();
        computed = true;
      }
      return zenith;
    }

    public double elevation() {
      if (!computed) {
        calcAzEl();
        computed = true;
      }
      return 90 - zenith;
    }

    private void calcAzEl() {
      double jct = AstroTime.julianCenturyTime(dateTime);
      SunTime st = new SunTime(jct);
      double eqTime = st.equationOfTime();
      double theta = st.sunDeclination();
      double solarTimeFix = eqTime + 4.0 * lon - 3600 * zone.getTotalSeconds();
      // double earthRadVec = sunRadVector(jt);
      double trueSolarTime =
          AstroTime.minutesDay(dateTime.toLocalTime()) + solarTimeFix;
      while (trueSolarTime > 1440) {
        trueSolarTime -= 1440;
      }
      double hourAngle = trueSolarTime / 4.0 - 180.0;
      if (hourAngle < -180) {
        hourAngle += 360.0;
      }
      double haRad = toRadians(hourAngle);
      double csz = sin(toRadians(lat)) * sin(toRadians(theta)) +
                   cos(toRadians(lat)) * cos(toRadians(theta)) * cos(haRad);
      if (csz > 1.0) {
        csz = 1.0;
      } else if (csz < -1.0) {
        csz = -1.0;
      }

      zenith = toDegrees(acos(csz));
      double azDenom = cos(toRadians(lat)) * sin(toRadians(zenith));
      if (abs(azDenom) > 0.001) {
        double azRad = (sin(toRadians(lat)) * cos(toRadians(zenith)) -
                        sin(toRadians(theta))) /
                       azDenom;
        if (abs(azRad) > 1.0) {
          if (azRad < 0) {
            azRad = -1.0;
          } else {
            azRad = 1.0;
          }
        }
        azimuth = 180.0 - toDegrees(acos(azRad));
        if (hourAngle > 0.0) {
          azimuth = -azimuth;
        }
      } else {
        if (lat > 0.0) {
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
        double te = tan(toRadians(exoatmElevation));
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
}

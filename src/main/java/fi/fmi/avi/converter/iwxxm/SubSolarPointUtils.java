package fi.fmi.avi.converter.iwxxm;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utility for calculating the sub-solar point (point on Earth where the Sun is directly overhead)
 * based on the <a href="https://gml.noaa.gov/grad/solcalc/calcdetails.html">NOAA Solar Calculator</a> algorithm.
 * <p>
 * The Equation of Time is converted to longitude to yield an exact match to
 * <a href="https://github.com/wmo-im/iwxxm/blob/v3.0-dev/IWXXM/examples/spacewx-A2-5.xml">WMO's IWXXM example</a>.
 */
public final class SubSolarPointUtils {

    public static final double DAYLIGHT_SIDE_RADIUS_KM = 10_100.0;

    // Unit conversion constants
    private static final double DEG_PER_CIRCLE = 360.0;
    private static final double RAD_PER_DEG = Math.PI / 180.0;
    private static final double DEG_PER_RAD = 180.0 / Math.PI;
    private static final double MINUTES_PER_HOUR = (double) ChronoUnit.HOURS.getDuration().toMinutes();
    private static final double SECONDS_PER_HOUR = (double) ChronoUnit.HOURS.getDuration().getSeconds();
    private static final double SECONDS_PER_DAY = (double) ChronoUnit.DAYS.getDuration().getSeconds();
    private static final double MILLIS_PER_DAY = (double) ChronoUnit.DAYS.getDuration().toMillis();

    // Julian calendar constants
    /**
     * Julian Day at Unix epoch (1970-01-01T00:00:00Z).
     */
    private static final double JULIAN_DAY_UNIX_EPOCH = 2_440_587.5;
    /**
     * Julian Day at J2000.0 epoch (2000-01-01 12:00 TT).
     */
    private static final double JULIAN_DAY_J2000 = 2_451_545.0;
    /**
     * Days per Julian century.
     */
    private static final double DAYS_PER_JULIAN_CENTURY = 36_525.0;

    // Geometric mean longitude of the Sun (degrees)
    private static final double L0_AT_J2000_DEG = 280.46646;
    private static final double L0_DEG_PER_CENTURY = 36_000.76983;
    private static final double L0_QUADRATIC_COEFF = 0.0003032;

    // Mean anomaly of the Sun (degrees)
    private static final double M_AT_J2000_DEG = 357.52911;
    private static final double M_DEG_PER_CENTURY = 35_999.05029;
    private static final double M_QUADRATIC_COEFF = -0.0001537;

    // Equation of center coefficients
    private static final double C1 = 1.914602;
    private static final double C2 = -0.004817;
    private static final double C3 = -0.000014;
    private static final double C4 = 0.019993;
    private static final double C5 = -0.000101;
    private static final double C6 = 0.000289;

    // Apparent longitude correction constants (degrees)
    private static final double ABERRATION_LAMBDA_APP_DEG = -0.00569;
    private static final double NUTATION_SCALE_DEG = -0.00478;

    // Lunar-node longitude Ω (degrees)
    private static final double OMEGA_AT_J2000_DEG = 125.04;
    private static final double OMEGA_DEG_PER_CENTURY = -1_934.136;

    // Obliquity ε coefficients (degrees)
    private static final double E0 = 23.439291;
    private static final double E1 = -0.0130042;
    private static final double E2 = -1.64e-7;
    private static final double E3 = 5.04e-7;
    private static final double EPS_CORR = 0.00256;

    // Equation of Time constant (degrees)
    private static final double ABERRATION_EOT_DEG = 0.0057183;

    // Conversion factors
    private static final double DEG_TO_TIME_MINUTES = 4.0; // Degrees to minutes of time (360° / 1440 min)
    private static final double FACTOR_2 = 2.0; // For 2M and 3M trigonometric terms
    private static final double FACTOR_3 = 3.0;
    private static final double HALF_CIRCLE_DEG = 180.0; // For GHA to longitude conversion

    private SubSolarPointUtils() {
    }

    /**
     * Computes the sub-solar point (latitude and longitude where the Sun is directly overhead)
     * for the given time.
     *
     * @param instant       the time for which to calculate the sub-solar point
     * @param decimalPlaces number of decimal places to round to (0-15)
     * @return array containing [latitude, longitude] in degrees, rounded
     */
    public static double[] computeSubSolarPoint(final Instant instant, final int decimalPlaces) {
        final long epochSecond = instant.getEpochSecond();
        final double hourUtc = (epochSecond % SECONDS_PER_DAY) / SECONDS_PER_HOUR;

        // Julian Day and centuries since J2000.0 epoch
        final double julianDay = instant.toEpochMilli() / MILLIS_PER_DAY + JULIAN_DAY_UNIX_EPOCH;
        final double T = (julianDay - JULIAN_DAY_J2000) / DAYS_PER_JULIAN_CENTURY;

        // Geometric mean longitude L0 and mean anomaly M (degrees)
        final double L0 = wrap360(L0_AT_J2000_DEG + L0_DEG_PER_CENTURY * T + L0_QUADRATIC_COEFF * T * T);
        final double M = wrap360(M_AT_J2000_DEG + M_DEG_PER_CENTURY * T + M_QUADRATIC_COEFF * T * T);

        // Equation of center C and true ecliptic longitude (degrees)
        final double Mr = M * RAD_PER_DEG;
        final double C = (C1 + C2 * T + C3 * T * T) * Math.sin(Mr)
                + (C4 + C5 * T) * Math.sin(FACTOR_2 * Mr)
                + C6 * Math.sin(FACTOR_3 * Mr);
        final double trueLon = L0 + C;

        // Nutation in longitude Δψ and apparent longitude λ_app (degrees)
        final double omegaDeg = OMEGA_AT_J2000_DEG + OMEGA_DEG_PER_CENTURY * T;
        final double dPsiDeg = NUTATION_SCALE_DEG * Math.sin(omegaDeg * RAD_PER_DEG);
        final double lambdaApp = trueLon + ABERRATION_LAMBDA_APP_DEG + dPsiDeg;

        // Mean obliquity ε0 and true obliquity ε (degrees)
        final double eps0 = E0 + E1 * T + E2 * T * T + E3 * T * T * T;
        final double eps = eps0 + EPS_CORR * Math.cos(omegaDeg * RAD_PER_DEG);

        // Right ascension α and declination δ (radians, then converted)
        final double lamR = lambdaApp * RAD_PER_DEG;
        final double epsR = eps * RAD_PER_DEG;
        final double alpha = Math.atan2(Math.cos(epsR) * Math.sin(lamR), Math.cos(lamR));
        final double delta = Math.asin(Math.sin(epsR) * Math.sin(lamR));

        // Equation of Time in minutes
        double alphaDeg = alpha * DEG_PER_RAD;
        alphaDeg = wrap360(alphaDeg);
        double eotDeg = L0 - ABERRATION_EOT_DEG - alphaDeg + dPsiDeg * Math.cos(eps0 * RAD_PER_DEG);
        eotDeg = wrap180(eotDeg);
        final double eotMinutes = DEG_TO_TIME_MINUTES * eotDeg;

        // Sub-solar longitude from Greenwich Hour Angle (GHA)
        final double tstMinutes = MINUTES_PER_HOUR * hourUtc + eotMinutes;
        final double ghaDeg = tstMinutes / DEG_TO_TIME_MINUTES;
        final double lonDeg = wrap180(HALF_CIRCLE_DEG - ghaDeg);

        // Sub-solar latitude equals solar declination
        final double latDeg = delta * DEG_PER_RAD;

        return new double[]{round(latDeg, decimalPlaces), round(lonDeg, decimalPlaces)};
    }

    public static double[] computeSubSolarPoint(final Instant instant) {
        return computeSubSolarPoint(instant, 2);
    }

    private static double round(final double value, final int decimalPlaces) {
        if (decimalPlaces < 0 || decimalPlaces > 15) {
            throw new IllegalArgumentException("Decimal places must be between 0 and 15");
        }
        final double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }

    /**
     * Wraps an angle to the range [0, 360).
     *
     * @param degrees the angle in degrees
     * @return the wrapped angle
     */
    private static double wrap360(final double degrees) {
        return (degrees % DEG_PER_CIRCLE + DEG_PER_CIRCLE) % DEG_PER_CIRCLE;
    }

    /**
     * Wraps an angle to the range [-180, 180).
     *
     * @param degrees the angle in degrees
     * @return the wrapped angle
     */
    private static double wrap180(final double degrees) {
        return ((degrees + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
    }
}
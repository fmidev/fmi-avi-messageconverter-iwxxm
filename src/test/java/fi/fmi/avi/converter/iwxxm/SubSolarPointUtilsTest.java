package fi.fmi.avi.converter.iwxxm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SubSolarPointUtilsTest {

    private static final double TOLERANCE = 0.01;

    private final String timestamp;
    private final double expLat;
    private final double expLon;

    public SubSolarPointUtilsTest(final String timestamp, final double expLat, final double expLon) {
        this.timestamp = timestamp;
        this.expLat = expLat;
        this.expLon = expLon;
    }

    @Parameterized.Parameters(name = "{index}: {0} → lat={1} lon={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"2016-11-08T00:06:00Z", -16.63, 174.44},
                {"2016-11-08T01:00:00Z", -16.64, 160.94},
                {"2016-11-08T07:00:00Z", -16.71, 70.94},
                {"2016-11-08T13:00:00Z", -16.78, -19.05},
                {"2016-11-08T19:00:00Z", -16.86, -109.05},
                {"2016-11-09T01:00:00Z", -16.93, 160.96},
                {"2020-11-08T01:00:00Z", -16.65, 160.94},
                {"2025-10-27T06:00:00Z", -12.88, 85.97},
                {"2025-10-27T12:00:00Z", -12.96, -4.04},
        });
    }

    @Test
    public void matches_expected_lat_lon() {
        final ZonedDateTime time = ZonedDateTime.parse(timestamp);
        final double[] latLon = SubSolarPointUtils.computeSubSolarPoint(time.toInstant());

        assertEquals("latitude", expLat, latLon[0], TOLERANCE);
        assertEquals("longitude", expLon, latLon[1], TOLERANCE);
    }

}

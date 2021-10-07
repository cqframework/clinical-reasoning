package org.opencds.cqf.cql.evaluator.measure.helper;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

public class DateHelperTest {

    @Test
    public void testResolveRequestDateWithTime() throws Exception {
        String date = "2019-01-17T12:30:00";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
        assertEquals(resolvedDateStart, resolvedDateEnd);

        Calendar calendar = Calendar.getInstance();
    }

    @Test
    public void testResolveRequestDateOffset() throws Exception {
        String date = "2019-01-01T22:00:00.0-06:00";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
        assertEquals(resolvedDateStart, resolvedDateEnd);

        String[] zones = TimeZone.getAvailableIDs(ZoneOffset.of("-06:00").getTotalSeconds() * 1000);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zones[0]));
    }

    @Test
    public void testResolveRequestDateWithZOffset() throws Exception {
        String date = "2017-01-01T00:00:00.000Z";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
        assertEquals(resolvedDateStart, resolvedDateEnd);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testResolveRequestOnlyDate() throws Exception {
        String date = "2017-01-01";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);


        Calendar calendar = Calendar.getInstance();
    }

    @Test
    public void testResolveRequestOnlyYear() throws Exception {
        String date = "2017";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);

        Calendar calendar = Calendar.getInstance();
    }
}
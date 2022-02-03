package org.opencds.cqf.cql.evaluator.measure.helper;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Date;

import org.testng.annotations.Test;

// TODO: These tests are only partially complete. We need to actually verify that resolved dates are correct.
public class DateHelperTest {

    @Test
    public void testResolveRequestDateWithTime() throws Exception {
        String date = "2019-01-17T12:30:00";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
        assertEquals(resolvedDateStart, resolvedDateEnd);
    }

    @Test
    public void testResolveRequestDateOffset() throws Exception {
        String date = "2019-01-01T22:00:00.0-06:00";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
        assertEquals(resolvedDateStart, resolvedDateEnd);
    }

    @Test
    public void testResolveRequestDateWithZOffset() throws Exception {
        String date = "2017-01-01T00:00:00.000Z";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
        assertEquals(resolvedDateStart, resolvedDateEnd);
    }

    @Test
    public void testResolveRequestOnlyDate() throws Exception {
        String date = "2017-01-01";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
    }

    @Test
    public void testResolveRequestOnlyYear() throws Exception {
        String date = "2017";
        Date resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        Date resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
    }
}
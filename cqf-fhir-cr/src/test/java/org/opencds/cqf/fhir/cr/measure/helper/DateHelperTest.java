package org.opencds.cqf.fhir.cr.measure.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// TODO: These tests are only partially complete. We need to actually verify that resolved dates are
// correct.
class DateHelperTest {

    @Test
    void resolveRequestDateWithTime() throws Exception {
        String date = "2019-01-17T12:30:00";
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
        assertEquals(resolvedDateStart, resolvedDateEnd);
    }

    @Test
    void resolveRequestDateOffset() throws Exception {
        String date = "2019-01-01T22:00:00.0-06:00";
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
        assertEquals(resolvedDateStart, resolvedDateEnd);
    }

    @Test
    void resolveRequestDateWithZOffset() throws Exception {
        String date = "2017-01-01T00:00:00.000Z";
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
        assertEquals(resolvedDateStart, resolvedDateEnd);
    }

    @Test
    void resolveRequestOnlyDate() throws Exception {
        String date = "2017-01-01";
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
    }

    @Test
    void resolveRequestOnlyYear() throws Exception {
        String date = "2017";
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertTrue(resolvedDateStart != null);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertTrue(resolvedDateEnd != null);
    }
}

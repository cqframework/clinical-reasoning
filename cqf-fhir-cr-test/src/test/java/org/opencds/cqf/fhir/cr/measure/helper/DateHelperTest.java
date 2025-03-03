package org.opencds.cqf.fhir.cr.measure.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// TODO: These tests are only partially complete. We need to actually verify that resolved dates are
// correct.
class DateHelperTest {

    @ParameterizedTest
    @ValueSource(strings = {"2019-01-17T12:30:00", "2019-01-01T22:00:00.0-06:00", "2017-01-01T00:00:00.000Z"})
    void resolveRequestDateWithTime(String date) {
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertNotNull(resolvedDateStart);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertNotNull(resolvedDateEnd);
        assertEquals(resolvedDateStart, resolvedDateEnd);
    }

    @Test
    void resolveRequestOnlyDate() {
        String date = "2017-01-01";
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertNotNull(resolvedDateStart);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertNotNull(resolvedDateEnd);
    }

    @Test
    void resolveRequestOnlyYear() {
        String date = "2017";
        var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
        assertNotNull(resolvedDateStart);

        var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
        assertNotNull(resolvedDateEnd);
    }
}

package org.opencds.cqf.cql.evaluator.measure.helper;

import java.util.Date;

import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

public class DateHelperTest {
    @Test
    public void testResolveRequestDateWithTime() throws Exception {
        String date = "2019-01-17T12:30:00";
        boolean start = false;
        Date resolvedDate = DateHelper.resolveRequestDate(date, start);
        assertTrue(resolvedDate != null);
    }

    @Test
    public void testResolveRequestDateOffset() throws Exception {
        String date = "2019-01-01T22:00:00.0-06:00";
        boolean start = false;
        Date resolvedDate = DateHelper.resolveRequestDate(date, start);
        assertTrue(resolvedDate != null);
    }

    @Test
    public void testResolveRequestDateWithSlashes() throws Exception {
        String date = "2019/01/01";
        boolean start = false;
        Date resolvedDate = DateHelper.resolveRequestDate(date, start);
        assertTrue(resolvedDate != null);
    }

    @Test
    public void testResolveRequestDateWithZOffset() throws Exception {
        String date = "2017-01-01T00:00:00.000Z";
        boolean start = false;
        Date resolvedDate = DateHelper.resolveRequestDate(date, start);
        assertTrue(resolvedDate != null);
    }
}
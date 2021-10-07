package org.opencds.cqf.cql.evaluator.measure.helper;

import java.util.Date;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

public class DateHelperTest {
    @Test
    public void testResolveRequestDate() throws Exception {
        String date = "2019-01-17T12:30:00";
        boolean start = false;
        Date resolvedDate = DateHelper.resolveRequestDate(date, start);
        assertTrue(resolvedDate != null);
    }
}
package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.Test;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;

public class R4DateHelperTest {

    @Test
    public void checkDate() {
        var date = new Interval(new Date("2019-01-01"), true, new Date("2019-12-31"), true);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var now = OffsetDateTime.now();
        // The CQL engine sets times with an unspecified offset to the _current_ system offset,
        // using the rules for the system default timezone. A given timezone may have variable
        // offsets from UTC (e.g. daylight savings time), and the current offset may be different
        // than the expected offset for a given date.
        var helper = new R4DateHelper();
        var period = helper.buildMeasurementPeriod(date);

        assertEquals(
                "2019-01-01", formatter.format(period.getStart().toInstant().atOffset(now.getOffset())));
        assertEquals("2019-12-31", formatter.format(period.getEnd().toInstant().atOffset(now.getOffset())));
    }

    @Test
    public void checkDateTime() {
        ZoneOffset offset = ZonedDateTime.now().getOffset();

        DateTime start = new DateTime("2019-01-01", offset);
        DateTime end = new DateTime("2019-12-31", offset);
        var date = new Interval(start, true, end, true);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // The CQL engine sets times with an unspecified offset to the _current_ system offset,
        // using the rules for the system default timezone. A given timezone may have variable
        // offsets from UTC (e.g. daylight savings time), and the current offset may be different
        // than the expected offset for a given date.
        var helper = new R4DateHelper();
        var period = helper.buildMeasurementPeriod(date);

        assertEquals(
                "2019-01-01", formatter.format(period.getStart().toInstant().atOffset(offset)));
        assertEquals("2019-12-31", formatter.format(period.getEnd().toInstant().atOffset(offset)));
    }

    @Test
    public void checkNull() {
        var helper = new R4DateHelper();
        try {
            helper.buildMeasurementPeriod(new Interval(new java.util.Date(), true, new java.util.Date(), true));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Measurement period should be an interval of CQL DateTime or Date"));
        }
    }
}

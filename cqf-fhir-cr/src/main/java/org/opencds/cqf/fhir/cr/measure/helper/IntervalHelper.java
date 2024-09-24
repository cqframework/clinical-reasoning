package org.opencds.cqf.fhir.cr.measure.helper;

import java.time.ZoneId;
import org.opencds.cqf.cql.engine.runtime.Interval;

/**
 * Helper class that leverages {@link DateHelper} to resolve measurement period start and end dates.
 * If a timezone is specified in a datetime, it's used. If not the timezone specified in the ZoneId
 * parameter is used..
 */
public class IntervalHelper {
    public static Interval buildMeasurementPeriod(String periodStart, String periodEnd, ZoneId clientTimezone) {
        // resolve the measurement period
        return new Interval(
                DateHelper.resolveRequestDate(periodStart, true, clientTimezone),
                true,
                DateHelper.resolveRequestDate(periodEnd, false, clientTimezone),
                true);
    }
}

package org.opencds.cqf.fhir.cr.measure.helper;

import org.opencds.cqf.cql.engine.runtime.Interval;
import java.time.ZoneId;

// LUKETODO: javadoc
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

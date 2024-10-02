package org.opencds.cqf.fhir.cr.measure.r4.utils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;
import org.hl7.fhir.r4.model.Period;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class R4DateHelper {

    public Period buildMeasurementPeriod(ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        Interval measurementPeriod = buildMeasurementPeriodInterval(periodStart, periodEnd);
        return buildMeasurementPeriod(measurementPeriod);
    }

    public Interval buildMeasurementPeriodInterval(ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        return new Interval(convertToDateTime(periodStart), true, convertToDateTime(periodEnd), true);
    }

    public Period buildMeasurementPeriod(Interval measurementPeriodInterval) {
        Period period = new Period();
        if (measurementPeriodInterval.getStart() instanceof DateTime) {
            DateTime dtStart = (DateTime) measurementPeriodInterval.getStart();
            DateTime dtEnd = (DateTime) measurementPeriodInterval.getEnd();

            final java.util.Date periodStartJavaUtilDate = convertToJavaUtilDateKeepOffset(dtStart);
            final java.util.Date periodEndJavaUtilDate = convertToJavaUtilDateKeepOffset(dtEnd);

//            period.setStart(periodStartJavaUtilDate)
//                  .setEnd(periodEndJavaUtilDate);

            period.setStart(dtStart.toJavaDate()).setEnd(dtEnd.toJavaDate());
        } else if (measurementPeriodInterval.getStart() instanceof Date) {
            Date dStart = (Date) measurementPeriodInterval.getStart();
            Date dEnd = (Date) measurementPeriodInterval.getEnd();

            // LUKETODO:  do we need to do anything different here?
            period.setStart(dStart.toJavaDate()).setEnd(dEnd.toJavaDate());
        } else {
            throw new IllegalArgumentException("Measurement period should be an interval of CQL DateTime or Date");
        }
        return period;
    }

    // This is such an incredibly nasty hack
    private java.util.Date convertToJavaUtilDateKeepOffset(DateTime dateTime) {
        final OffsetDateTime offsetDateTime = dateTime.getDateTime();

        final java.util.Date javaUtilDateWithDefaultTimezone = java.util.Date.from(offsetDateTime.toInstant());

        final TimeZone oldDefaultTimezone = TimeZone.getDefault();
        try {
            final TimeZone timeZoneUtc = TimeZone.getTimeZone(ZoneOffset.UTC);
            final Calendar calendarUtc = Calendar.getInstance(timeZoneUtc);
            calendarUtc.setTime(javaUtilDateWithDefaultTimezone);

            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            final java.util.Date javaUtilDateWithIntendedTimezone = calendarUtc.getTime();

            return javaUtilDateWithIntendedTimezone;
        } finally {
            // We MUST do this no matter what, or we've screwed up the default timezone
            TimeZone.setDefault(oldDefaultTimezone);
        }
    }

    private DateTime convertToDateTime(ZonedDateTime zonedDateTime) {
        final OffsetDateTime offsetDateTime = zonedDateTime.toOffsetDateTime();
        return new DateTime(offsetDateTime);
    }
}

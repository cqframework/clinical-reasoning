package org.opencds.cqf.fhir.cr.measure.helper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.runtime.Precision;

/**
 * Helper class to resolve measurement period start and end dates. If a timezone
 * is specified in a
 * datetime, it's used. If not the timezone of the local system is used.
 */
public class DateHelper {
    private DateHelper() {}

    public static DateTime resolveRequestDate(String date, boolean start) {
        // ISO Instance Format
        if (date.contains("Z")) {
            var offset = Instant.parse(date).atOffset(ZoneOffset.UTC);
            return new DateTime(offset);
        }

        // ISO Offset Format
        boolean isOffsetDateString = date.contains("T") && date.contains(".") && date.contains("-");
        if (isOffsetDateString) {
            var offset = OffsetDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return new DateTime(offset);
        }

        // Local DateTime
        if (date.contains("T")) {
            var offset = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
            return new DateTime(offset);
        }

        return resolveDate(start, date);
    }

    private static DateTime resolveDate(boolean start, String dateString) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();

        String[] dissect = dateString.split("-");
        List<Integer> dateVals = new ArrayList<>();
        for (String dateElement : dissect) {
            dateVals.add(Integer.parseInt(dateElement));
        }

        if (dateVals.isEmpty()) {
            throw new IllegalArgumentException("Invalid date");
        }

        calendar.setTimeZone(TimeZone.getDefault());

        // Set year
        calendar.set(Calendar.YEAR, dateVals.get(0));

        // Set month (if defined, otherwise earliest if start, latest if end)
        if (dateVals.size() > 1) {
            // java.util.Date months are zero based, hence the negative 1 -- 2014-01 ==
            // February 2014
            calendar.set(Calendar.MONTH, dateVals.get(1) - 1);
        } else {
            if (start) {
                calendar.set(Calendar.MONTH, 0);
            } else {
                calendar.set(Calendar.MONTH, 11);
            }
        }

        // Set day (if defined, otherwise earliest if start, latest if end)
        if (dateVals.size() > 2) calendar.set(Calendar.DAY_OF_MONTH, dateVals.get(2));
        else {
            if (start) {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            } else {
                // get last day of month for end period
                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.DATE, -1);
            }
        }

        // Set time (not defined if we got this far. Earliest if start, latest if end)
        if (start) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
        }

        // TODO: Seems like we might want set the precision appropriately here?
        var offset = calendar.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        return new DateTime(offset);
    }

    public static Interval buildMeasurementPeriodInterval(ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        return new Interval(convertToDateTime(periodStart), true, convertToDateTime(periodEnd), true);
    }

    private static DateTime convertToDateTime(ZonedDateTime zonedDateTime) {
        final OffsetDateTime offsetDateTime = zonedDateTime.toOffsetDateTime();
        final DateTime convertedDateTime = new DateTime(offsetDateTime);
        convertedDateTime.setPrecision(Precision.SECOND);
        return convertedDateTime;
    }
}

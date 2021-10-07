package org.opencds.cqf.cql.evaluator.measure.helper;

import java.text.DateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import joptsimple.internal.Strings;

public class DateHelper {

    // Helper class to resolve period dates
    public static Date resolveRequestDate(String date, boolean start) {
        boolean isOffsetDateString = date.contains("T") && date.contains(".") && date.contains("-");
        if (isOffsetDateString) {
            return Date.from(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)));
        }
        // split it up - support dashes or slashes
        String dateString = date;
        String timeZoneString = "";
        if (date.contains("T")) {
            dateString = date.substring(0, date.indexOf("T"));
            timeZoneString = date.substring(date.indexOf("T") + 1, date.length());
        }
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        resolveDate(start, dateString, calendar);
        resolveTime(timeZoneString, calendar);
        return calendar.getTime();
    }

    private static void resolveDate(boolean start, String dateString, Calendar calendar) {
        String[] dissect = dateString.contains("-") ? dateString.split("-") : dateString.split("/");
        List<Integer> dateVals = new ArrayList<>();
        for (String dateElement : dissect) {
            dateVals.add(Integer.parseInt(dateElement));
        }

        if (dateVals.isEmpty()) {
            throw new IllegalArgumentException("Invalid date");
        }

        // for now support dates up to day precision
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.set(Calendar.YEAR, dateVals.get(0));
        if (dateVals.size() > 1) {
            // java.util.Date months are zero based, hence the negative 1 -- 2014-01 == February 2014
            calendar.set(Calendar.MONTH, dateVals.get(1) - 1);
        }
        if (dateVals.size() > 2)
            calendar.set(Calendar.DAY_OF_MONTH, dateVals.get(2));
        else {
            if (start) {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            }
            else {
                // get last day of month for end period
                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.DATE, -1);
            }
        }
    }

    private static void resolveTime(String timeZoneString, Calendar calendar) {
        if (!Strings.isNullOrEmpty(timeZoneString)) {
            String[] dissectTimeZone = timeZoneString.split(":");
            List<Integer> timeVals = new ArrayList<>();
            for (String timeElement : dissectTimeZone) {
                timeVals.add(Integer.parseInt(timeElement));
            }
            calendar.set(Calendar.HOUR_OF_DAY, timeVals.get(0));
            calendar.set(Calendar.MINUTE, timeVals.get(1));
            calendar.set(Calendar.SECOND, timeVals.get(2));
        }
    }
}
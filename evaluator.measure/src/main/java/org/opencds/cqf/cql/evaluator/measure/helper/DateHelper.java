package org.opencds.cqf.cql.evaluator.measure.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import joptsimple.internal.Strings;

public class DateHelper {

    // Helper class to resolve period dates
    public static Date resolveRequestDate(String date, boolean start) {
        // split it up - support dashes or slashes
        String dateString = date;
        String timeZoneString = "";
        if (date.contains("T")) {
            dateString = date.substring(0, date.indexOf("T"));
            timeZoneString = date.substring(date.indexOf("T") + date.length());
        }
        String[] dissect = dateString.contains("-") ? dateString.split("-") : dateString.split("/");
        List<Integer> dateVals = new ArrayList<>();
        for (String dateElement : dissect) {
            dateVals.add(Integer.parseInt(dateElement));
        }

        if (dateVals.isEmpty()) {
            throw new IllegalArgumentException("Invalid date");
        }

        // for now support dates up to day precision
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
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

        if (!Strings.isNullOrEmpty(timeZoneString)) {
            String[] dissectTimeZone = dateString.split(":");
            List<Integer> timeVals = new ArrayList<>();
            Integer offset = null;
            for (String timeElement : dissectTimeZone) {
                if (timeElement.contains("-")) {
                    timeVals.add(Integer.parseInt(timeElement.substring(0, timeElement.indexOf("-"))));
                    offset = Integer.parseInt(timeElement.substring(timeElement.indexOf("-"), timeElement.length()));
                } else {
                    timeVals.add(Integer.parseInt(timeElement));
                }
            }

            calendar.set(Calendar.HOUR_OF_DAY, timeVals.get(0));
            calendar.set(Calendar.MINUTE, timeVals.get(1));
            calendar.set(Calendar.SECOND, timeVals.get(2));
            if (offset != null) {
                calendar.set(Calendar.ZONE_OFFSET, offset);
            }
        }
        return calendar.getTime();
    }
}
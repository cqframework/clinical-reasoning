package org.opencds.cqf.fhir.cr.measure.r4.utils;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.hl7.fhir.r4.model.Period;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.runtime.Precision;

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

            period.setStart(dtStart.toJavaDate()).setEnd(dtEnd.toJavaDate());
        } else if (measurementPeriodInterval.getStart() instanceof Date) {
            Date dStart = (Date) measurementPeriodInterval.getStart();
            Date dEnd = (Date) measurementPeriodInterval.getEnd();

            period.setStart(dStart.toJavaDate()).setEnd(dEnd.toJavaDate());
        } else {
            throw new IllegalArgumentException("Measurement period should be an interval of CQL DateTime or Date");
        }
        return period;
    }

    private DateTime convertToDateTime(ZonedDateTime zonedDateTime) {
        final OffsetDateTime offsetDateTime = zonedDateTime.toOffsetDateTime();
        final DateTime convertedDateTime = new DateTime(offsetDateTime);
        convertedDateTime.setPrecision(Precision.SECOND);
        return convertedDateTime;
    }

}

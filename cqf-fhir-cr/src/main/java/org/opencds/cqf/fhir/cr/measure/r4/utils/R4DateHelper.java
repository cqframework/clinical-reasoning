package org.opencds.cqf.fhir.cr.measure.r4.utils;

import org.hl7.fhir.r4.model.Period;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.helper.DateHelper;

public class R4DateHelper {

    public Interval buildMeasurementPeriodInterval(String periodStart, String periodEnd) {
        // resolve the measurement period
        return new Interval(
                DateHelper.resolveRequestDate(periodStart, true),
                true,
                DateHelper.resolveRequestDate(periodEnd, false),
                true);
    }

    public Period buildMeasurementPeriod(String periodStart, String periodEnd) {
        Interval measurementPeriod = buildMeasurementPeriodInterval(periodStart, periodEnd);
        return buildMeasurementPeriod(measurementPeriod);
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
}

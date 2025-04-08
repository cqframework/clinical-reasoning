package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.runtime.Precision;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IPeriodAdapter;

public class DateHelper {

    private final FhirVersionEnum fhirVersion;

    public DateHelper(FhirVersionEnum fhirVersion) {
        this.fhirVersion = fhirVersion;
    }

    public Interval buildMeasurementPeriodInterval(ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        return new Interval(convertToDateTime(periodStart), true, convertToDateTime(periodEnd), true);
    }

    public IPeriodAdapter buildMeasurementPeriod(Interval measurementPeriodInterval) {
        IPeriodAdapter period = IAdapterFactory.forFhirVersion(fhirVersion).createPeriod();
        if (measurementPeriodInterval.getStart() instanceof DateTime dtStart) {
            DateTime dtEnd = (DateTime) measurementPeriodInterval.getEnd();

            period.setStart(dtStart.toJavaDate()).setEnd(dtEnd.toJavaDate());
        } else if (measurementPeriodInterval.getStart() instanceof Date dStart) {
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

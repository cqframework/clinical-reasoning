package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.fhir.utility.monad.Either3;

/**
 * Parameters class to manage input parameters for care-gaps service
 */
public class R4CareGapsParameters {
    private ZonedDateTime periodStart;
    private ZonedDateTime periodEnd;
    private String subject;
    private List<String> status;
    private List<Either3<IdType, String, CanonicalType>> measure;
    private boolean notDocument;

    public void setPeriodStart(ZonedDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public ZonedDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodEnd(ZonedDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public ZonedDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    public void setNotDocument(boolean notDocument) {
        this.notDocument = notDocument;
    }

    public boolean isNotDocument() {
        return notDocument;
    }

    public void setMeasure(List<Either3<IdType, String, CanonicalType>> measure) {
        this.measure = measure;
    }

    public List<Either3<IdType, String, CanonicalType>> getMeasure() {
        return measure;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }

    public List<String> getStatus() {
        return status;
    }
}

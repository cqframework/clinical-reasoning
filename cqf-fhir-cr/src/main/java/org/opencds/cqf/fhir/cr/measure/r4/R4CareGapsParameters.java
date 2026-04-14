package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.ZonedDateTime;
import java.util.List;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;

/**
 * Parameters class to manage input parameters for care-gaps service
 */
public class R4CareGapsParameters {
    private ZonedDateTime periodStart;
    private ZonedDateTime periodEnd;
    private String subject;
    private List<String> status;
    private List<MeasureReference> measure;
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

    public void setMeasure(List<MeasureReference> measure) {
        this.measure = measure;
    }

    public List<MeasureReference> getMeasure() {
        return measure;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }

    public List<String> getStatus() {
        return status;
    }
}

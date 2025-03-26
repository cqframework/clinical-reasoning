package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.IEitherMeasureAdapter;
import org.opencds.cqf.fhir.utility.monad.Either3;

import java.time.ZonedDateTime;
import java.util.List;

// LUKETODO:  figure out what to do with this
/**
 * Parameters class to manage input parameters for care-gaps service
 */
public class CareGapsParameters {
    private ZonedDateTime periodStart;
    private ZonedDateTime periodEnd;
    private String subject;
    private List<String> status;
    private List<IEitherMeasureAdapter> measure;
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

    public void setMeasure(List<IEitherMeasureAdapter> measure) {
        this.measure = measure;
    }

//    public List<? extends Either3<? extends IIdType, String, ? extends IPrimitiveType<String>>> getMeasure() {
//        return measure;
//    }

    public List<IEitherMeasureAdapter> getMeasure() {
        return measure;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }

    public List<String> getStatus() {
        return status;
    }
}

package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This enum represents the values defined inline for the reportType parameter of the Measure
 * $evaluate-measure operation.
 *
 * The values currently (as of R4) aren't bound to the same ValueSet as the MeasureReport type so
 * that's why this is defined separately. The intent is that they will bound to the same ValueSet in
 * the future.
 */
public enum MeasureEvalType {
    SUBJECT(
            "subject",
            "Subject",
            "An evaluation generating an individual report that provides information on the performance for a given measure with respect to a single subject"),
    SUBJECTLIST(
            "subject-list",
            "Subject List",
            "An evaluation generating a subject list report that includes a listing of subjects that satisfied each population criteria in the measure."),
    PATIENT(
            "patient",
            "Patient",
            "An evaluation generating an individual report that provides information on the performance for a given measure with respect to a single patient"),
    PATIENTLIST(
            "patient-list",
            "Patient List",
            "An evaluation generating a patient list report that includes a listing of patients that satisfied each population criteria in the measure"),
    POPULATION(
            "population",
            "Population",
            "An evaluation generating a summary report that returns the number of subjects in each population criteria for the measure");

    private String code;
    private String display;
    private String definition;

    MeasureEvalType(String code, String display, String definition) {
        this.code = code;
        this.display = display;
        this.definition = definition;
    }

    private static final Map<String, MeasureEvalType> lookup = new HashMap<>();

    static {
        for (MeasureEvalType mpt : MeasureEvalType.values()) {
            lookup.put(mpt.toCode(), mpt);
        }
    }

    // This method can be used for reverse lookup purposes
    public static Optional<MeasureEvalType> fromCode(String code) {
        MeasureEvalType evalType = lookup.get(code);
        if (code != null && evalType == null) {
            throw new InvalidRequestException("ReportType: %s, is not an accepted EvalType value.".formatted(code));
        }
        return Optional.ofNullable(evalType);
    }

    public static MeasureEvalType getEvalType(MeasureEvalType evalType, String reportType, List<String> subjectIds) {
        if (evalType == null) {
            return MeasureEvalType.fromCode(reportType)
                    .orElse(
                            subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
                                    ? MeasureEvalType.POPULATION
                                    : MeasureEvalType.SUBJECT);
        }
        return evalType;
    }

    public String getSystem() {
        return null;
    }

    public String toCode() {
        return this.code;
    }

    public String getDisplay() {
        return this.display;
    }

    public String getDefinition() {
        return this.definition;
    }
}

package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class was created to separate R4 accepted MeasureEvalTypes from other dSTU3 compatible values (Patient, Patient-List)
 */
public enum R4MeasureEvalType {
    SUBJECT(
            "subject",
            "Subject",
            "An evaluation generating an individual report that provides information on the performance for a given measure with respect to a single subject"),
    SUBJECTLIST(
            "subject-list",
            "Subject List",
            "An evaluation generating a subject list report that includes a listing of subjects that satisfied each population criteria in the measure."),
    POPULATION(
            "population",
            "Population",
            "An evaluation generating a summary report that returns the number of subjects in each population criteria for the measure");

    private String code;
    private String display;
    private String definition;

    R4MeasureEvalType(String code, String display, String definition) {
        this.code = code;
        this.display = display;
        this.definition = definition;
    }

    private static final Map<String, R4MeasureEvalType> lookup = new HashMap<>();

    static {
        for (R4MeasureEvalType mpt : R4MeasureEvalType.values()) {
            lookup.put(mpt.toCode(), mpt);
        }
    }

    // This method can be used for reverse lookup purposes
    public static Optional<R4MeasureEvalType> fromCode(String code) {
        R4MeasureEvalType evalType = lookup.get(code);
        if (code != null && evalType == null) {
            throw new UnsupportedOperationException(
                    "ReportType: %s, is not an accepted R4 EvalType value.".formatted(code));
        }
        return Optional.ofNullable(evalType);
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

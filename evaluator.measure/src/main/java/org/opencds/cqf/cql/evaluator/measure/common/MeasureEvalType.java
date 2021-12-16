package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.HashMap;
import java.util.Map;

/**
 * This enum represents the values defined inline for the reportType parameter of the Measure $evaluate-measure operation.
 * 
 * The values currently (as of R4) aren't bound to the same ValueSet as the MeasureReport type so that's why this is defined
 * separately. The intent is that they will bound to the same ValueSet in the future.
 */
public enum MeasureEvalType {
    SUBJECT("subject", "Subject","An evaluation generating an individual report that provides information on the performance for a given measure with respect to a single subject"), 
    SUBJECTLIST("subject-list", "Subject List","An evaluation generating a subject list report that includes a listing of subjects that satisfied each population criteria in the measure."),
    PATIENT("patient", "Patient","An evaluation generating an individual report that provides information on the performance for a given measure with respect to a single patient"), 
    PATIENTLIST("patient-list", "Patient List","An evaluation generating a patient list report that includes a listing of patients that satisfied each population criteria in the measure"), 
    POPULATION("population", "Population","An evaluation generating a summary report that returns the number of subjects in each population criteria for the measure");

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
    public static MeasureEvalType fromCode(String code) {
        if (code != null && !code.isEmpty()) {
            if (lookup.containsKey(code)) {
                return lookup.get(code);
            }
        }

        return null;
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

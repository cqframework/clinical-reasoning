package org.opencds.cqf.fhir.cr.measure;

/**
 * Indicate whether a given {@link org.opencds.cqf.fhir.cr.measure.common.StratifierDef} is
 * criteria or value based.
 */
public enum MeasureStratifierType {
    CRITERIA("criteria"),
    VALUE("value"),
    NON_SUBJECT_VALUE("non-subject-value");

    private final String textValue;

    MeasureStratifierType(String textValue) {
        this.textValue = textValue;
    }

    public String getTextValue() {
        return textValue;
    }
}

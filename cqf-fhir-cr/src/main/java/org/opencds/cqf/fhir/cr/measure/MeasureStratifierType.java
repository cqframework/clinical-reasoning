package org.opencds.cqf.fhir.cr.measure;

// LUKETODO:  maybe make this is a grid to include component vs non-component
/**
 * Indicate whether a given {@link org.opencds.cqf.fhir.cr.measure.common.StratifierDef} is
 * criteria or value based.
 */
public enum MeasureStratifierType {
    CRITERIA("criteria"),
    VALUE("value");

    private final String textValue;

    MeasureStratifierType(String textValue) {
        this.textValue = textValue;
    }

    public String getTextValue() {
        return textValue;
    }
}

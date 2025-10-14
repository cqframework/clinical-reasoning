package org.opencds.cqf.fhir.cr.measure;

// LUKETODO:  do we need text values?
// LUKETODO:  javadoc
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

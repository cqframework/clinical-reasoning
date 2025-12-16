package org.opencds.cqf.fhir.cr.measure;

import org.opencds.cqf.fhir.cr.measure.common.def.report.StratifierReportDef;

/**
 * Indicate whether a given {@link StratifierReportDef} is
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

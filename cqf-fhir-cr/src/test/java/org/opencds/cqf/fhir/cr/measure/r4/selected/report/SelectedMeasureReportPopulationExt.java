package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;

public class SelectedMeasureReportPopulationExt extends Selected<Extension, SelectedMeasureReportPopulation> {

    public SelectedMeasureReportPopulationExt(Extension value, SelectedMeasureReportPopulation parent) {
        super(value, parent);
    }

    // -------------------------
    // resultBoolean (given)
    // -------------------------
    public SelectedMeasureReportPopulationExt hasBooleanResult(Boolean expected) {
        Boolean actual = this.value.getExtension().stream()
                .filter(e -> "resultBoolean".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(v -> v instanceof BooleanType)
                .map(v -> ((BooleanType) v).booleanValue())
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual);
        return this;
    }

    // -------------------------
    // resultString
    // -------------------------
    public SelectedMeasureReportPopulationExt hasStringResult(String expected) {
        String actual = this.value.getExtension().stream()
                .filter(e -> "resultString".equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(v -> v instanceof StringType)
                .map(v -> ((StringType) v).getValue())
                .findFirst()
                .orElse(null);

        assertEquals(expected, actual);
        return this;
    }

    // -------------------------
    // resultResourceId (stored as valueString)
    // -------------------------
    public SelectedMeasureReportPopulationExt hasResourceIdResult(String expectedResourceId) {
        String actual = this.value.getExtension().stream()
                .filter(e -> "resultList".equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing resultList"))
                .getExtension()
                .stream()
                .filter(item -> "itemResourceId".equals(item.getUrl()))
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .filter(expectedResourceId::equals)
                .findFirst()
                .orElse(null);

        assertEquals(expectedResourceId, actual);
        return this;
    }

    public SelectedMeasureReportPopulationExt hasListBooleanResult(String expectedResourceId) {
        String actual = this.value.getExtension().stream()
                .filter(e -> "resultList".equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing resultList"))
                .getExtension()
                .stream()
                .filter(item -> "itemBoolean".equals(item.getUrl()))
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .filter(expectedResourceId::equals)
                .findFirst()
                .orElse(null);

        assertEquals(expectedResourceId, actual);
        return this;
    }
}

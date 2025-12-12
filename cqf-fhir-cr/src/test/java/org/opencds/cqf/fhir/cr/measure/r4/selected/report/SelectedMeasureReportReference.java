package org.opencds.cqf.fhir.cr.measure.r4.selected.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CRITERIA_REFERENCE_URL;

import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Selected;

public class SelectedMeasureReportReference extends Selected<Reference, SelectedMeasureReport> {

    public SelectedMeasureReportReference(Reference value, SelectedMeasureReport parent) {
        super(value, parent);
    }

    public SelectedMeasureReportReference hasNoDuplicateExtensions() {
        var exts = this.value().getExtensionsByUrl(EXT_CRITERIA_REFERENCE_URL);
        var extsCount = exts.size();
        var distinctExtCount = (int) exts.stream()
                .map(t -> ((StringType) t.getValue()).getValue())
                .distinct()
                .count();
        assertEquals(extsCount, distinctExtCount, "extension contain duplicate values");
        return this;
    }

    public SelectedMeasureReportReference referenceHasExtension(String extValueRef) {
        var ex = this.value().getExtensionsByUrl(EXT_CRITERIA_REFERENCE_URL);
        if (ex.isEmpty()) {
            throw new IllegalStateException(
                    "no evaluated resource extensions were found, and expected %s".formatted(extValueRef));
        }
        String foundRef = null;
        for (Extension extension : ex) {
            assert extension.getValue() instanceof StringType;
            StringType extValue = (StringType) extension.getValue();
            if (extValue.getValue().equals(extValueRef)) {
                foundRef = extValue.getValue();
                break;
            }
        }
        assertNotNull(foundRef);
        return this;
    }

    public SelectedMeasureReportReference hasEvaluatedResourceReferenceCount(int count) {
        assertEquals(count, this.value().getExtension().size());
        return this;
    }

    // Hmm.. may need to rethink this one a bit.
    public SelectedMeasureReportReference hasPopulations(String... population) {
        var ex = this.value().getExtensionsByUrl(EXT_CRITERIA_REFERENCE_URL);
        if (ex.isEmpty()) {
            throw new IllegalStateException(
                    "no evaluated resource extensions were found, and expected %s".formatted(population.length));
        }

        @SuppressWarnings("unchecked")
        var set = ex.stream()
                .map(x -> ((IPrimitiveType<String>) x.getValue()).getValue())
                .collect(Collectors.toSet());

        for (var p : population) {
            assertTrue(
                    set.contains(p),
                    "population: %s was not found in the evaluated resources criteria reference extension list"
                            .formatted(p));
        }

        return this;
    }
}

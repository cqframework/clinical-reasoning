package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefValidationContext;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefValidator;
import org.opencds.cqf.fhir.cr.measure.common.ValidationIssue;
import org.opencds.cqf.fhir.cr.measure.common.ValidationResult;
import org.opencds.cqf.fhir.cr.measure.common.ValidationSeverity;
import org.opencds.cqf.fhir.utility.search.Searches;

/**
 * Validates that ValueSets referenced in the Measure's CQL library are available in the repository.
 * Extracts ValueSet canonical URLs from the library's {@code dataRequirement.codeFilter.valueSet}
 * entries and performs an existence check (no expansion). Produces {@code VALUESET_UNAVAILABLE}
 * warnings since external terminology services may still resolve them at evaluation time.
 */
public class R4ValueSetAvailabilityValidator implements MeasureDefValidator {

    public static final String VALUESET_UNAVAILABLE = "VALUESET_UNAVAILABLE";

    @Override
    public ValidationResult validate(MeasureDefValidationContext context) {
        var result = new ValidationResult();
        var measure = (Measure) context.measure();

        if (!measure.hasLibrary() || measure.getLibrary().isEmpty()) {
            return result;
        }

        // Collect ValueSet references from the primary library's dataRequirement
        var valueSetUrls = collectValueSetUrls(measure, context);

        for (var vsUrl : valueSetUrls) {
            var bundle = context.repository().search(Bundle.class, ValueSet.class, Searches.byUrl(vsUrl), null);

            if (bundle.getEntry().isEmpty()) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.WARNING,
                        VALUESET_UNAVAILABLE,
                        "ValueSet '%s' referenced by the Measure library is not available in the repository"
                                .formatted(vsUrl),
                        "Ensure the ValueSet with URL '%s' is loaded in the repository or available via a configured terminology service."
                                .formatted(vsUrl)));
            }
        }

        return result;
    }

    private Set<String> collectValueSetUrls(Measure measure, MeasureDefValidationContext context) {
        var valueSetUrls = new HashSet<String>();

        for (var libraryCanonical : measure.getLibrary()) {
            var url = libraryCanonical.asStringValue();
            var bundle = context.repository().search(Bundle.class, Library.class, Searches.byCanonical(url), null);

            if (bundle.getEntry().isEmpty()) {
                continue;
            }

            var library = (Library) bundle.getEntryFirstRep().getResource();
            for (var dataReq : library.getDataRequirement()) {
                for (var codeFilter : dataReq.getCodeFilter()) {
                    if (codeFilter.hasValueSet()) {
                        valueSetUrls.add(codeFilter.getValueSet());
                    }
                }
            }
        }

        return valueSetUrls;
    }
}

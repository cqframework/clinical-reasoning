package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefValidationContext;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefValidator;
import org.opencds.cqf.fhir.cr.measure.common.ValidationIssue;
import org.opencds.cqf.fhir.cr.measure.common.ValidationResult;
import org.opencds.cqf.fhir.cr.measure.common.ValidationSeverity;
import org.opencds.cqf.fhir.utility.search.Searches;

/**
 * Validates that the CQL libraries referenced by a Measure are resolvable in the repository.
 * Checks the primary library and follows transitive {@code relatedArtifact} dependencies of type
 * {@code depends-on}. Produces {@code LIBRARY_NOT_FOUND} errors for any missing library.
 */
public class R4CqlLibraryValidator implements MeasureDefValidator {

    public static final String LIBRARY_NOT_FOUND = "LIBRARY_NOT_FOUND";

    @Override
    public ValidationResult validate(MeasureDefValidationContext context) {
        var result = new ValidationResult();
        var measure = (Measure) context.measure();

        if (!measure.hasLibrary() || measure.getLibrary().isEmpty()) {
            result.addIssue(new ValidationIssue(
                    ValidationSeverity.ERROR,
                    LIBRARY_NOT_FOUND,
                    "Measure '%s' does not have a primary library specified".formatted(measure.getUrl()),
                    "Add a library reference to the Measure resource using the 'library' element.",
                    "Measure.library"));
            return result;
        }

        var checked = new HashSet<String>();
        for (var libraryCanonical : measure.getLibrary()) {
            var url = libraryCanonical.asStringValue();
            validateLibraryExists(url, context, result, checked);
        }

        return result;
    }

    private void validateLibraryExists(
            String canonicalUrl, MeasureDefValidationContext context, ValidationResult result, Set<String> checked) {

        if (!checked.add(canonicalUrl)) {
            return;
        }

        var bundle = context.repository().search(Bundle.class, Library.class, Searches.byCanonical(canonicalUrl), null);

        if (bundle.getEntry().isEmpty()) {
            result.addIssue(new ValidationIssue(
                    ValidationSeverity.ERROR,
                    LIBRARY_NOT_FOUND,
                    "Library with canonical URL '%s' was not found in the repository".formatted(canonicalUrl),
                    "Ensure the Library resource with URL '%s' is loaded in the repository.".formatted(canonicalUrl),
                    "Measure.library"));
            return;
        }

        // Check transitive dependencies via relatedArtifact
        var library = (Library) bundle.getEntryFirstRep().getResource();
        for (var relatedArtifact : library.getRelatedArtifact()) {
            if (relatedArtifact.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON
                            && relatedArtifact.hasResource()
                            && relatedArtifact.getResource().startsWith("Library/")
                    || isLibraryCanonical(relatedArtifact)) {
                validateLibraryExists(relatedArtifact.getResource(), context, result, checked);
            }
        }
    }

    private static boolean isLibraryCanonical(RelatedArtifact relatedArtifact) {
        return relatedArtifact.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON
                && relatedArtifact.hasResource()
                && !relatedArtifact.getResource().startsWith("http://hl7.org/fhir/Library/FHIR-ModelInfo");
    }
}

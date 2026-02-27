package org.opencds.cqf.fhir.cr.implementationguide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

/**
 * Tests for ImplementationGuideProcessor verifying that the enriched
 * $data-requirements path is properly wired through CrSettings.
 */
class ImplementationGuideProcessorTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    /**
     * Verifies end-to-end wiring: ImplementationGuideProcessor with CrSettings
     * creates DataRequirementsProcessor with a router, activating the enriched path
     * that produces CRMI extensions on RelatedArtifacts.
     */
    @Test
    void dataRequirements_withCrSettings_usesEnrichedPath() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create a ValueSet dependency
        var vs = new ValueSet();
        vs.setId("ValueSet/test-vs");
        vs.setUrl("http://example.org/ValueSet/test-vs");
        vs.setVersion("1.0.0");
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        vs.setName("Test VS");
        vs.addExtension(Constants.PACKAGE_SOURCE, new StringType("test.package#1.0.0"));
        repository.update(vs);

        // Create a Library that depends on the ValueSet
        var lib = new Library();
        lib.setId("Library/test-lib");
        lib.setUrl("http://example.org/Library/test-lib");
        lib.setVersion("1.0.0");
        lib.setStatus(Enumerations.PublicationStatus.ACTIVE);
        lib.setName("Test Library");
        lib.addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/ValueSet/test-vs");
        repository.update(lib);

        // Create IG with the Library as a definition resource
        var ig = new ImplementationGuide();
        ig.setId("ImplementationGuide/test-ig");
        ig.setUrl("http://example.org/ImplementationGuide/test-ig");
        ig.setVersion("1.0.0");
        ig.setStatus(Enumerations.PublicationStatus.ACTIVE);
        ig.setName("TestIG");
        ig.setPackageId("test.ig");
        ig.getDefinition().addResource().setReference(new Reference("Library/test-lib"));
        repository.update(ig);

        // Create processor with CrSettings (which wires up the router)
        var processor = new ImplementationGuideProcessor(repository, CrSettings.getDefault());

        var result = processor.dataRequirements(Eithers.forMiddle3(ig.getIdElement()), newParameters(fhirContext));

        assertInstanceOf(Library.class, result);
        var library = (Library) result;
        assertEquals("module-definition", library.getType().getCodingFirstRep().getCode());

        // Verify enriched path produced dependencies with CRMI extensions
        assertTrue(!library.getRelatedArtifact().isEmpty(), "Should have gathered dependencies");

        for (var ra : library.getRelatedArtifact()) {
            // Each dependency should have crmi-dependencyRole extension
            var roleExtensions = ra.getExtensionsByUrl(Constants.CRMI_DEPENDENCY_ROLE);
            assertTrue(!roleExtensions.isEmpty(), "Should have dependency role extension");
        }
    }

    /**
     * Verifies that without CrSettings (default constructor),
     * the processor still works via the non-enriched path.
     */
    @Test
    void dataRequirements_withDefaultConstructor_usesOriginalPath() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var ig = new ImplementationGuide();
        ig.setId("ImplementationGuide/test-ig");
        ig.setUrl("http://example.org/ImplementationGuide/test-ig");
        ig.setVersion("1.0.0");
        ig.setStatus(Enumerations.PublicationStatus.ACTIVE);
        ig.setName("TestIG");
        ig.setPackageId("test.ig");
        repository.update(ig);

        // Default constructor — should still work but via original path
        var processor = new ImplementationGuideProcessor(repository);

        var result = processor.dataRequirements(Eithers.forMiddle3(ig.getIdElement()), newParameters(fhirContext));

        assertInstanceOf(Library.class, result);
        var library = (Library) result;
        assertEquals("module-definition", library.getType().getCodingFirstRep().getCode());
    }
}

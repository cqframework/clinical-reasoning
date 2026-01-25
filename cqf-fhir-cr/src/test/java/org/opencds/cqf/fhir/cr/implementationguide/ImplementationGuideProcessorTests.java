package org.opencds.cqf.fhir.cr.implementationguide;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class ImplementationGuideProcessorTests {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final IParser jsonParser = fhirContextR4.newJsonParser();
    private InMemoryFhirRepository repository;

    @BeforeEach
    void setup() {
        repository = new InMemoryFhirRepository(fhirContextR4);
        // Load the ImplementationGuide test resource
        var ig = (ImplementationGuide)
                jsonParser.parseResource(ImplementationGuideProcessorTests.class.getResourceAsStream(
                        "ImplementationGuide-hl7.fhir.us.core-6-1-0.json"));
        repository.update(ig);

        // Load the Library test resource
        var library = (Library) jsonParser.parseResource(
                ImplementationGuideProcessorTests.class.getResourceAsStream("Library-uscore-vsp-6-1-0.json"));
        repository.update(library);
    }

    @Test
    void testDataRequirements() {
        var processor = new ImplementationGuideProcessor(repository);

        IBaseResource result = processor.dataRequirements(
                Eithers.forMiddle3(Ids.newId(fhirContextR4, "ImplementationGuide", "hl7.fhir.us.core")), null);

        assertNotNull(result);
        assertTrue(result instanceof Library, "Result should be a Library (module-definition)");

        var library = (Library) result;
        assertNotNull(library.getType(), "Library should have a type");
        assertTrue(
                library.getType().getCodingFirstRep().getCode().contains("module-definition"),
                "Library type should be module-definition");
    }

    @Test
    void testDataRequirementsWithDirectResource() {
        var processor = new ImplementationGuideProcessor(repository);

        // Read the IG from the repository first
        var ig = (ImplementationGuide) repository.read(
                ImplementationGuide.class, Ids.newId(fhirContextR4, "ImplementationGuide", "hl7.fhir.us.core"));

        assertNotNull(ig, "ImplementationGuide should exist in repository");

        // Now test data requirements with the IG resource directly
        IBaseResource result = processor.dataRequirements(ig, null);

        assertNotNull(result);
        assertTrue(result instanceof Library, "Result should be a Library (module-definition)");

        var library = (Library) result;
        assertNotNull(library.getType(), "Library should have a type");
        assertTrue(
                library.getType().getCodingFirstRep().getCode().contains("module-definition"),
                "Library type should be module-definition");
    }

    @Test
    void testResolveImplementationGuide() {
        var processor = new ImplementationGuideProcessor(repository);

        var result = processor.resolveImplementationGuide(
                Eithers.forMiddle3(Ids.newId(fhirContextR4, "ImplementationGuide", "hl7.fhir.us.core")));

        assertNotNull(result);
        assertTrue(result instanceof ImplementationGuide);
        var ig = (ImplementationGuide) result;
        assertNotNull(ig.getUrl());
        assertNotNull(ig.getVersion());
    }
}

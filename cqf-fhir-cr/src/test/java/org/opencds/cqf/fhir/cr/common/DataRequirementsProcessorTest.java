package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class DataRequirementsProcessorTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    @Test
    void constructor_withRepository_createsProcessor() {
        var repository = new InMemoryFhirRepository(fhirContext);
        var processor = new DataRequirementsProcessor(repository);

        assertNotNull(processor);
    }

    @Test
    void constructor_withEvaluationSettings_createsProcessor() {
        var repository = new InMemoryFhirRepository(fhirContext);
        var processor = new DataRequirementsProcessor(repository, EvaluationSettings.getDefault());

        assertNotNull(processor);
    }

    @Test
    void getDataRequirements_withNullParameters_usesDefaults() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var ig = new ImplementationGuide();
        ig.setId("ImplementationGuide/test-ig");
        ig.setUrl("http://example.org/ImplementationGuide/test-ig");
        ig.setVersion("1.0.0");
        ig.setStatus(Enumerations.PublicationStatus.ACTIVE);
        ig.setName("TestIG");
        ig.setPackageId("test.ig");
        repository.update(ig);

        var processor = new DataRequirementsProcessor(repository);

        // Pass null parameters — should use defaults and not throw
        var result = processor.getDataRequirements(ig, null);

        assertNotNull(result);
        assertInstanceOf(Library.class, result);
    }
}

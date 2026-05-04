package org.opencds.cqf.fhir.cr.bundle;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.common.ValidateProcessor;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class BundleProcessorTests {

    @Test
    void processor_validate_processor_provided() {
        var ctx = FhirContext.forR4();
        var repository = new InMemoryFhirRepository(ctx);
        var validateProcessor = new ValidateProcessor(ctx);
        var processor = new BundleProcessor(repository, validateProcessor);

        Bundle toValidate = new Bundle();

        OperationOutcome result = (OperationOutcome) processor.validate(toValidate, null, null);
        Assertions.assertNotNull(result);
    }

    @Test
    void procssor_validate_processor_not_provided() {
        var ctx = FhirContext.forR4();
        var repository = new InMemoryFhirRepository(ctx);
        var processor = new BundleProcessor(repository, null);

        Bundle toValidate = new Bundle();

        OperationOutcome result = (OperationOutcome) processor.validate(toValidate, null, null);
        Assertions.assertNotNull(result);
    }
}

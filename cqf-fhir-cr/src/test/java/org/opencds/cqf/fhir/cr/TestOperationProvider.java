package org.opencds.cqf.fhir.cr;

import ca.uhn.fhir.context.FhirContext;
import org.opencds.cqf.fhir.utility.repository.operations.RepositoryOperationProvider;

public class TestOperationProvider {
    public static RepositoryOperationProvider newProvider(FhirContext fhirContext) {
        return new RepositoryOperationProvider(fhirContext, new ActivityDefinitionProcessorFactory(), null, null, null);
    }
}

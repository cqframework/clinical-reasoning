package org.opencds.cqf.fhir.utility.repository.operations;

import ca.uhn.fhir.repository.Repository;

public interface IPlanDefinitionProcessorFactory {
    IPlanDefinitionProcessor create(Repository repository);
}

package org.opencds.cqf.fhir.utility.repository.operations;

import org.opencds.cqf.fhir.api.Repository;

public interface IPlanDefinitionProcessorFactory {
    IPlanDefinitionProcessor create(Repository repository);
}

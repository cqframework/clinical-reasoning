package org.opencds.cqf.fhir.utility.repository.operations;

import ca.uhn.fhir.repository.IRepository;

public interface IPlanDefinitionProcessorFactory {
    IPlanDefinitionProcessor create(IRepository repository);
}

package org.opencds.cqf.fhir.utility.repository.operations;

import ca.uhn.fhir.repository.IRepository;

@FunctionalInterface
public interface IActivityDefinitionProcessorFactory {
    IActivityDefinitionProcessor create(IRepository repository);
}

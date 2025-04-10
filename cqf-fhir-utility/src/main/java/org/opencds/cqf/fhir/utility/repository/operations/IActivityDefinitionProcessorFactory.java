package org.opencds.cqf.fhir.utility.repository.operations;

import ca.uhn.fhir.repository.Repository;

@FunctionalInterface
public interface IActivityDefinitionProcessorFactory {
    IActivityDefinitionProcessor create(Repository repository);
}

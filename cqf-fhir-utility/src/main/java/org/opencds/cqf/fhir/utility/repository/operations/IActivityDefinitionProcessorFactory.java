package org.opencds.cqf.fhir.utility.repository.operations;

import org.opencds.cqf.fhir.api.Repository;

@FunctionalInterface
public interface IActivityDefinitionProcessorFactory {
    IActivityDefinitionProcessor create(Repository repository);
}

package org.opencds.cqf.fhir.utility.repository.operations;

import ca.uhn.fhir.repository.IRepository;

public interface IQuestionnaireResponseProcessorFactory {
    IQuestionnaireResponseProcessor create(IRepository repository);
}

package org.opencds.cqf.fhir.utility.repository.operations;

import ca.uhn.fhir.repository.Repository;

public interface IQuestionnaireResponseProcessorFactory {
    IQuestionnaireResponseProcessor create(Repository repository);
}

package org.opencds.cqf.fhir.utility.repository.operations;

import org.opencds.cqf.fhir.api.Repository;

public interface IQuestionnaireResponseProcessorFactory {
    IQuestionnaireResponseProcessor create(Repository repository);
}

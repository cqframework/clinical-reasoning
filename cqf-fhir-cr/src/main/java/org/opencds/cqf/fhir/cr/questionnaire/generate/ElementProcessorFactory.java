package org.opencds.cqf.fhir.cr.questionnaire.generate;

import org.opencds.cqf.fhir.api.Repository;

public class ElementProcessorFactory {
    public IElementProcessor create(Repository repository) {
        switch (repository.fhirContext().getVersion().getVersion()) {
            case DSTU3:
                return new org.opencds.cqf.fhir.cr.questionnaire.generate.dstu3.ElementProcessor(repository);
            case R4:
                return new org.opencds.cqf.fhir.cr.questionnaire.generate.r4.ElementProcessor(repository);
            case R5:
                return new org.opencds.cqf.fhir.cr.questionnaire.generate.r5.ElementProcessor(repository);
            default:
                return null;
        }
    }
}

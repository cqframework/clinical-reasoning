package org.opencds.cqf.fhir.cr.questionnaire.dstu3.processor;

import static org.opencds.cqf.fhir.cr.questionnaire.BaseQuestionnaireProcessor.castOrThrow;

import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.dstu3.SearchHelper;

public class ResolveProcessor {
    final Repository repository;

    public ResolveProcessor(Repository repository) {
        this.repository = repository;
    }

    public <C extends IPrimitiveType<String>> Questionnaire resolve(
            IIdType id, C canonical, IBaseResource questionnaire) {
        var baseQuestionnaire = questionnaire;
        if (baseQuestionnaire == null) {
            baseQuestionnaire = id != null
                    ? repository.read(Questionnaire.class, id)
                    : (Questionnaire) SearchHelper.searchRepositoryByCanonical(repository, canonical);
        }
        return castOrThrow(
                        baseQuestionnaire,
                        Questionnaire.class,
                        "The Questionnaire passed to repository was not a valid instance of Questionnaire.class")
                .orElse(null);
    }
}

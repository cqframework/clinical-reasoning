package org.opencds.cqf.fhir.cr.hapi.config;

import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.activitydefinition.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.cr.hapi.common.*;
import org.opencds.cqf.fhir.cr.library.LibraryProcessor;
import org.opencds.cqf.fhir.cr.plandefinition.PlanDefinitionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.QuestionnaireProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.QuestionnaireResponseProcessor;
import org.opencds.cqf.fhir.cr.valueset.ValueSetProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrProcessorConfig {
    @Bean
    IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new ActivityDefinitionProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    IPlanDefinitionProcessorFactory planDefinitionProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new PlanDefinitionProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    IQuestionnaireProcessorFactory questionnaireProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new QuestionnaireProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    IQuestionnaireResponseProcessorFactory questionnaireResponseProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new QuestionnaireResponseProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    ILibraryProcessorFactory libraryProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new LibraryProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    IValueSetProcessorFactory valueSetProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new ValueSetProcessor(repositoryFactory.create(rd), evaluationSettings);
    }
}

package ca.uhn.fhir.cr.config;

import ca.uhn.fhir.cr.common.IRepositoryFactory;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.activitydefinition.ActivityDefinitionProcessor;
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
    ca.uhn.fhir.cr.common.IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new ActivityDefinitionProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    ca.uhn.fhir.cr.common.IPlanDefinitionProcessorFactory planDefinitionProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new PlanDefinitionProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    ca.uhn.fhir.cr.common.IQuestionnaireProcessorFactory questionnaireProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new QuestionnaireProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    ca.uhn.fhir.cr.common.IQuestionnaireResponseProcessorFactory questionnaireResponseProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new QuestionnaireResponseProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    ca.uhn.fhir.cr.common.ILibraryProcessorFactory libraryProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new LibraryProcessor(repositoryFactory.create(rd), evaluationSettings);
    }

    @Bean
    ca.uhn.fhir.cr.common.IValueSetProcessorFactory valueSetProcessorFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {
        return rd -> new ValueSetProcessor(repositoryFactory.create(rd), evaluationSettings);
    }
}

package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import java.util.Optional;
import org.opencds.cqf.fhir.cql.Engines.EngineInitializationContext;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.activitydefinition.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.cr.hapi.common.IActivityDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireResponseProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.cr.library.LibraryProcessor;
import org.opencds.cqf.fhir.cr.plandefinition.PlanDefinitionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.QuestionnaireProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.QuestionnaireResponseProcessor;
import org.opencds.cqf.fhir.cr.valueset.ValueSetProcessor;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.npm.NpmConfigDependencySubstitutor;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrProcessorConfig {
    @Bean
    IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            EvaluationSettings evaluationSettings) {

        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);

            return new ActivityDefinitionProcessor(
                    repository,
                    evaluationSettings,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            evaluationSettings));
        };
    }

    @Bean
    IPlanDefinitionProcessorFactory planDefinitionProcessorFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings) {

        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);

            return new PlanDefinitionProcessor(
                    repository,
                    evaluationSettings,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            evaluationSettings),
                    terminologyServerClientSettings);
        };
    }

    @Bean
    IQuestionnaireProcessorFactory questionnaireProcessorFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            EvaluationSettings evaluationSettings) {

        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);

            return new QuestionnaireProcessor(
                    repository,
                    evaluationSettings,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            evaluationSettings));
        };
    }

    @Bean
    IQuestionnaireResponseProcessorFactory questionnaireResponseProcessorFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            EvaluationSettings evaluationSettings) {

        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);
            return new QuestionnaireResponseProcessor(
                    repository,
                    evaluationSettings,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            evaluationSettings));
        };
    }

    @Bean
    ILibraryProcessorFactory libraryProcessorFactory(
            IRepositoryFactory repositoryFactory,
            Optional<NpmPackageLoader> optNpmPackageLoader,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings) {

        return requestDetails -> {
            var repository = repositoryFactory.create(requestDetails);
            return new LibraryProcessor(
                    repository,
                    evaluationSettings,
                    new EngineInitializationContext(
                            repository,
                            NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(optNpmPackageLoader),
                            evaluationSettings),
                    terminologyServerClientSettings);
        };
    }

    @Bean
    IValueSetProcessorFactory valueSetProcessorFactory(
            IRepositoryFactory repositoryFactory,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings) {
        return rd -> new ValueSetProcessor(
                repositoryFactory.create(rd), evaluationSettings, terminologyServerClientSettings);
    }
}

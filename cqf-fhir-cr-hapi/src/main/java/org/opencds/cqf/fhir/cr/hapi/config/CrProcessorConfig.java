package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import java.util.List;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.activitydefinition.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.cr.cql.CqlProcessor;
import org.opencds.cqf.fhir.cr.graphdefinition.GraphDefinitionProcessor;
import org.opencds.cqf.fhir.cr.graphdefinition.apply.ApplyRequestBuilder;
import org.opencds.cqf.fhir.cr.hapi.common.HapiArtifactDiffProcessor;
import org.opencds.cqf.fhir.cr.hapi.common.IActivityDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.ICqlProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionApplyRequestBuilderFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IImplementationGuideProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireResponseProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.cr.implementationguide.ImplementationGuideProcessor;
import org.opencds.cqf.fhir.cr.library.LibraryProcessor;
import org.opencds.cqf.fhir.cr.plandefinition.PlanDefinitionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.QuestionnaireProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.QuestionnaireResponseProcessor;
import org.opencds.cqf.fhir.cr.valueset.ValueSetProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SuppressWarnings("UnstableApiUsage")
@Configuration
@Import({CrBaseConfig.class})
public class CrProcessorConfig {
    @Bean
    ICqlProcessorFactory cqlProcessorFactory(IRepositoryFactory repositoryFactory, CrSettings crSettings) {
        return rd -> new CqlProcessor(repositoryFactory.create(rd), crSettings);
    }

    @Bean
    IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory(
            IRepositoryFactory repositoryFactory, CrSettings crSettings) {
        return rd -> new ActivityDefinitionProcessor(repositoryFactory.create(rd), crSettings);
    }

    @Bean
    IImplementationGuideProcessorFactory implementationGuideProcessorFactory(
            IRepositoryFactory repositoryFactory, CrSettings crSettings) {
        return rd -> new ImplementationGuideProcessor(repositoryFactory.create(rd), crSettings);
    }

    @Bean
    IPlanDefinitionProcessorFactory planDefinitionProcessorFactory(
            IRepositoryFactory repositoryFactory, CrSettings crSettings) {
        return rd -> new PlanDefinitionProcessor(repositoryFactory.create(rd), crSettings);
    }

    @Bean
    IQuestionnaireProcessorFactory questionnaireProcessorFactory(
            IRepositoryFactory repositoryFactory, CrSettings crSettings) {
        return rd -> new QuestionnaireProcessor(repositoryFactory.create(rd), crSettings);
    }

    @Bean
    IQuestionnaireResponseProcessorFactory questionnaireResponseProcessorFactory(
            IRepositoryFactory repositoryFactory, CrSettings crSettings) {
        return rd -> new QuestionnaireResponseProcessor(repositoryFactory.create(rd), crSettings);
    }

    @Bean
    ILibraryProcessorFactory libraryProcessorFactory(IRepositoryFactory repositoryFactory, CrSettings crSettings) {
        return rd -> {
            var repository = repositoryFactory.create(rd);
            return new LibraryProcessor(repository, crSettings, List.of(new HapiArtifactDiffProcessor(repository)));
        };
    }

    @Bean
    IValueSetProcessorFactory valueSetProcessorFactory(IRepositoryFactory repositoryFactory, CrSettings crSettings) {
        return rd -> new ValueSetProcessor(repositoryFactory.create(rd), crSettings);
    }

    @Bean
    IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory(
            IRepositoryFactory repositoryFactory, CrSettings crSettings) {
        return rd -> new GraphDefinitionProcessor(repositoryFactory.create(rd), crSettings);
    }

    @Bean
    IGraphDefinitionApplyRequestBuilderFactory graphDefinitionApplyRequestBuilderFactory(
            IRepositoryFactory repositoryFactory, EvaluationSettings evaluationSettings) {

        return rd -> new ApplyRequestBuilder(repositoryFactory.create(rd), evaluationSettings);
    }
}

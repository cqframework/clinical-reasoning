package org.opencds.cqf.fhir.cr.hapi.config.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IImplementationGuideProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r5.graphdefinition.GraphDefinitionDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r5.implementationguide.ImplementationGuideDataRequirements;
import org.opencds.cqf.fhir.cr.hapi.r5.library.LibraryDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r5.plandefinition.PlanDefinitionDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r5.questionnaire.QuestionnaireDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r5.valueset.ValueSetDataRequirementsProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class DataRequirementsOperationConfig {
    @Bean
    LibraryDataRequirementsProvider r5LibraryDataRequirementsProvider(
            ILibraryProcessorFactory libraryProcessorFactory) {
        return new LibraryDataRequirementsProvider(libraryProcessorFactory);
    }

    @Bean
    PlanDefinitionDataRequirementsProvider r5PlanDefinitionDataRequirementsProvider(
            IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        return new PlanDefinitionDataRequirementsProvider(planDefinitionProcessorFactory);
    }

    @Bean
    QuestionnaireDataRequirementsProvider r5QuestionnaireDataRequirementsProvider(
            IQuestionnaireProcessorFactory questionnaireFactory) {
        return new QuestionnaireDataRequirementsProvider(questionnaireFactory);
    }

    @Bean
    ValueSetDataRequirementsProvider r5ValueSetDataRequirementsProvider(IValueSetProcessorFactory valueSetFactory) {
        return new ValueSetDataRequirementsProvider(valueSetFactory);
    }

    @Bean
    GraphDefinitionDataRequirementsProvider r5GraphDefinitionDataRequirementsProvider(
            IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory) {
        return new GraphDefinitionDataRequirementsProvider(graphDefinitionProcessorFactory);
    }

    @Bean
    ImplementationGuideDataRequirements r5ImplementationGuideDataRequirementsProvider(
            IImplementationGuideProcessorFactory implementationGuideProcessorFactory) {
        return new ImplementationGuideDataRequirements(implementationGuideProcessorFactory);
    }

    @Bean(name = "dataRequirementsOperationLoader")
    public ProviderLoader dataRequirementsOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R5,
                        Arrays.asList(
                                LibraryDataRequirementsProvider.class,
                                PlanDefinitionDataRequirementsProvider.class,
                                QuestionnaireDataRequirementsProvider.class,
                                ValueSetDataRequirementsProvider.class,
                                GraphDefinitionDataRequirementsProvider.class,
                                ImplementationGuideDataRequirements.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

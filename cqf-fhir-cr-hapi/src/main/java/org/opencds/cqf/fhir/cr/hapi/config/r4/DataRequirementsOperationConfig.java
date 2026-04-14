package org.opencds.cqf.fhir.cr.hapi.config.r4;

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
import org.opencds.cqf.fhir.cr.hapi.r4.graphdefinition.GraphDefinitionDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.implementationguide.ImplementationGuideDataRequirements;
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.plandefinition.PlanDefinitionDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.questionnaire.QuestionnaireDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.valueset.ValueSetDataRequirementsProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class DataRequirementsOperationConfig {
    @Bean
    LibraryDataRequirementsProvider r4LibraryDataRequirementsProvider(
            ILibraryProcessorFactory libraryProcessorFactory) {
        return new LibraryDataRequirementsProvider(libraryProcessorFactory);
    }

    @Bean
    PlanDefinitionDataRequirementsProvider r4PlanDefinitionDataRequirementsProvider(
            IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        return new PlanDefinitionDataRequirementsProvider(planDefinitionProcessorFactory);
    }

    @Bean
    QuestionnaireDataRequirementsProvider r4QuestionnaireDataRequirementsProvider(
            IQuestionnaireProcessorFactory questionnaireFactory) {
        return new QuestionnaireDataRequirementsProvider(questionnaireFactory);
    }

    @Bean
    ValueSetDataRequirementsProvider r4ValueSetDataRequirementsProvider(IValueSetProcessorFactory valueSetFactory) {
        return new ValueSetDataRequirementsProvider(valueSetFactory);
    }

    @Bean
    GraphDefinitionDataRequirementsProvider r4GraphDefinitionDataRequirementsProvider(
            IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory) {
        return new GraphDefinitionDataRequirementsProvider(graphDefinitionProcessorFactory);
    }

    @Bean
    ImplementationGuideDataRequirements r4ImplementationGuideDataRequirementsProvider(
            IImplementationGuideProcessorFactory implementationGuideProcessorFactory) {
        return new ImplementationGuideDataRequirements(implementationGuideProcessorFactory);
    }

    @Bean(name = "dataRequirementsOperationLoader")
    public ProviderLoader dataRequirementsOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R4,
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

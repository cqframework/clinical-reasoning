package org.opencds.cqf.fhir.cr.hapi.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r4.graphdefinition.GraphDefinitionPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.plandefinition.PlanDefinitionPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.questionnaire.QuestionnairePackageProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.valueset.ValueSetPackageProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class PackageOperationConfig {
    @Bean
    PlanDefinitionPackageProvider r4PlanDefinitionPackageProvider(
            IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        return new PlanDefinitionPackageProvider(planDefinitionProcessorFactory);
    }

    @Bean
    QuestionnairePackageProvider r4QuestionnairePackageProvider(
            IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        return new QuestionnairePackageProvider(questionnaireProcessorFactory);
    }

    @Bean
    LibraryPackageProvider r4LibraryPackageProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        return new LibraryPackageProvider(libraryProcessorFactory);
    }

    @Bean
    ValueSetPackageProvider r4ValueSetPackageProvider(IValueSetProcessorFactory valueSetProcessorFactory) {
        return new ValueSetPackageProvider(valueSetProcessorFactory);
    }

    @Bean
    GraphDefinitionPackageProvider r4GraphDefinitionPackageProvider(
            IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory) {
        return new GraphDefinitionPackageProvider(graphDefinitionProcessorFactory);
    }

    @Bean(name = "packageOperationLoader")
    public ProviderLoader packageOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R4,
                        Arrays.asList(
                                LibraryPackageProvider.class,
                                QuestionnairePackageProvider.class,
                                PlanDefinitionPackageProvider.class,
                                ValueSetPackageProvider.class,
                                GraphDefinitionPackageProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

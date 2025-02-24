package org.opencds.cqf.fhir.cr.hapi.config.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.dstu3.library.LibraryPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.dstu3.plandefinition.PlanDefinitionPackageProvider;
import org.opencds.cqf.fhir.cr.hapi.dstu3.questionnaire.QuestionnairePackageProvider;
import org.opencds.cqf.fhir.cr.hapi.dstu3.valueset.ValueSetPackageProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class PackageOperationConfig {
    @Bean
    PlanDefinitionPackageProvider dstu3PlanDefinitionPackageProvider(
            IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        return new PlanDefinitionPackageProvider(planDefinitionProcessorFactory);
    }

    @Bean
    QuestionnairePackageProvider dstu3QuestionnairePackageProvider(
            IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        return new QuestionnairePackageProvider(questionnaireProcessorFactory);
    }

    @Bean
    LibraryPackageProvider dstu3LibraryPackageProvider(
            ILibraryProcessorFactory libraryProcessorFactory) {
        return new LibraryPackageProvider(libraryProcessorFactory);
    }

    @Bean
    ValueSetPackageProvider dstu3ValueSetPackageProvider(
            IValueSetProcessorFactory valueSetProcessorFactory) {
        return new ValueSetPackageProvider(valueSetProcessorFactory);
    }

    @Bean(name = "packageOperationLoader")
    public ProviderLoader packageOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.DSTU3,
                        Arrays.asList(
                                LibraryPackageProvider.class,
                                QuestionnairePackageProvider.class,
                                PlanDefinitionPackageProvider.class,
                                ValueSetPackageProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

package ca.uhn.fhir.cr.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.ILibraryProcessorFactory;
import ca.uhn.fhir.cr.common.IPlanDefinitionProcessorFactory;
import ca.uhn.fhir.cr.common.IQuestionnaireProcessorFactory;
import ca.uhn.fhir.cr.common.IValueSetProcessorFactory;
import ca.uhn.fhir.cr.config.CrProcessorConfig;
import ca.uhn.fhir.cr.config.ProviderLoader;
import ca.uhn.fhir.cr.config.ProviderSelector;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class PackageOperationConfig {
    @Bean
    ca.uhn.fhir.cr.r4.plandefinition.PlanDefinitionPackageProvider r4PlanDefinitionPackageProvider(
            IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        return new ca.uhn.fhir.cr.r4.plandefinition.PlanDefinitionPackageProvider(planDefinitionProcessorFactory);
    }

    @Bean
    ca.uhn.fhir.cr.r4.questionnaire.QuestionnairePackageProvider r4QuestionnairePackageProvider(
            IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        return new ca.uhn.fhir.cr.r4.questionnaire.QuestionnairePackageProvider(questionnaireProcessorFactory);
    }

    @Bean
    ca.uhn.fhir.cr.r4.library.LibraryPackageProvider r4LibraryPackageProvider(
            ILibraryProcessorFactory libraryProcessorFactory) {
        return new ca.uhn.fhir.cr.r4.library.LibraryPackageProvider(libraryProcessorFactory);
    }

    @Bean
    ca.uhn.fhir.cr.r4.valueset.ValueSetPackageProvider r4ValueSetPackageProvider(
            IValueSetProcessorFactory valueSetProcessorFactory) {
        return new ca.uhn.fhir.cr.r4.valueset.ValueSetPackageProvider(valueSetProcessorFactory);
    }

    @Bean(name = "packageOperationLoader")
    public ProviderLoader packageOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R4,
                        Arrays.asList(
                                ca.uhn.fhir.cr.r4.library.LibraryPackageProvider.class,
                                ca.uhn.fhir.cr.r4.questionnaire.QuestionnairePackageProvider.class,
                                ca.uhn.fhir.cr.r4.plandefinition.PlanDefinitionPackageProvider.class,
                                ca.uhn.fhir.cr.r4.valueset.ValueSetPackageProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

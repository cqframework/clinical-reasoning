package ca.uhn.fhir.cr.config.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
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
    ca.uhn.fhir.cr.dstu3.plandefinition.PlanDefinitionPackageProvider dstu3PlanDefinitionPackageProvider() {
        return new ca.uhn.fhir.cr.dstu3.plandefinition.PlanDefinitionPackageProvider();
    }

    @Bean
    ca.uhn.fhir.cr.dstu3.questionnaire.QuestionnairePackageProvider dstu3QuestionnairePackageProvider(
            IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        return new ca.uhn.fhir.cr.dstu3.questionnaire.QuestionnairePackageProvider(questionnaireProcessorFactory);
    }

    @Bean
    ca.uhn.fhir.cr.dstu3.library.LibraryPackageProvider dstu3LibraryPackageProvider() {
        return new ca.uhn.fhir.cr.dstu3.library.LibraryPackageProvider();
    }

    @Bean
    ca.uhn.fhir.cr.dstu3.valueset.ValueSetPackageProvider dstu3ValueSetPackageProvider(
            IValueSetProcessorFactory valueSetProcessorFactory) {
        return new ca.uhn.fhir.cr.dstu3.valueset.ValueSetPackageProvider(valueSetProcessorFactory);
    }

    @Bean(name = "packageOperationLoader")
    public ProviderLoader packageOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.DSTU3,
                        Arrays.asList(
                                ca.uhn.fhir.cr.dstu3.library.LibraryPackageProvider.class,
                                ca.uhn.fhir.cr.dstu3.questionnaire.QuestionnairePackageProvider.class,
                                ca.uhn.fhir.cr.dstu3.plandefinition.PlanDefinitionPackageProvider.class,
                                ca.uhn.fhir.cr.dstu3.valueset.ValueSetPackageProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

package ca.uhn.fhir.cr.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
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
public class DataRequirementsOperationConfig {
    @Bean
    ca.uhn.fhir.cr.r4.library.LibraryDataRequirementsProvider r4LibraryDataRequirementsProvider() {
        return new ca.uhn.fhir.cr.r4.library.LibraryDataRequirementsProvider();
    }

    @Bean
    ca.uhn.fhir.cr.r4.plandefinition.PlanDefinitionDataRequirementsProvider r4PlanDefinitionDataRequirementsProvider() {
        return new ca.uhn.fhir.cr.r4.plandefinition.PlanDefinitionDataRequirementsProvider();
    }

    @Bean
    ca.uhn.fhir.cr.r4.questionnaire.QuestionnaireDataRequirementsProvider r4QuestionnaireDataRequirementsProvider() {
        return new ca.uhn.fhir.cr.r4.questionnaire.QuestionnaireDataRequirementsProvider();
    }

    @Bean
    ca.uhn.fhir.cr.r4.valueset.ValueSetDataRequirementsProvider r4ValueSetDataRequirementsProvider(
            IValueSetProcessorFactory valueSetFactory) {
        return new ca.uhn.fhir.cr.r4.valueset.ValueSetDataRequirementsProvider(valueSetFactory);
    }

    @Bean(name = "dataRequirementsOperationLoader")
    public ProviderLoader dataRequirementsOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R4,
                        Arrays.asList(
                                ca.uhn.fhir.cr.r4.library.LibraryDataRequirementsProvider.class,
                                ca.uhn.fhir.cr.r4.plandefinition.PlanDefinitionDataRequirementsProvider.class,
                                ca.uhn.fhir.cr.r4.questionnaire.QuestionnaireDataRequirementsProvider.class,
                                ca.uhn.fhir.cr.r4.valueset.ValueSetDataRequirementsProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

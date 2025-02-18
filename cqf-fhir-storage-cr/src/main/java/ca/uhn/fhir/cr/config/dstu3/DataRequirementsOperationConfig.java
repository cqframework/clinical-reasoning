package ca.uhn.fhir.cr.config.dstu3;

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
public class DataRequirementsOperationConfig {
    @Bean
    ca.uhn.fhir.cr.dstu3.library.LibraryDataRequirementsProvider dstu3LibraryDataRequirementsProvider(
            ILibraryProcessorFactory libraryProcessorFactory) {
        return new ca.uhn.fhir.cr.dstu3.library.LibraryDataRequirementsProvider(libraryProcessorFactory);
    }

    @Bean
    ca.uhn.fhir.cr.dstu3.plandefinition.PlanDefinitionDataRequirementsProvider
            dstu3PlanDefinitionDataRequirementsProvider(
                    IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        return new ca.uhn.fhir.cr.dstu3.plandefinition.PlanDefinitionDataRequirementsProvider(
                planDefinitionProcessorFactory);
    }

    @Bean
    ca.uhn.fhir.cr.dstu3.questionnaire.QuestionnaireDataRequirementsProvider dstu3QuestionnaireDataRequirementsProvider(
            IQuestionnaireProcessorFactory questionnaireFactory) {
        return new ca.uhn.fhir.cr.dstu3.questionnaire.QuestionnaireDataRequirementsProvider(questionnaireFactory);
    }

    @Bean
    ca.uhn.fhir.cr.dstu3.valueset.ValueSetDataRequirementsProvider dstu3ValueSetDataRequirementsProvider(
            IValueSetProcessorFactory valueSetFactory) {
        return new ca.uhn.fhir.cr.dstu3.valueset.ValueSetDataRequirementsProvider(valueSetFactory);
    }

    @Bean(name = "dataRequirementsOperationLoader")
    public ProviderLoader dataRequirementsOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.DSTU3,
                        Arrays.asList(
                                ca.uhn.fhir.cr.dstu3.library.LibraryDataRequirementsProvider.class,
                                ca.uhn.fhir.cr.dstu3.plandefinition.PlanDefinitionDataRequirementsProvider.class,
                                ca.uhn.fhir.cr.dstu3.questionnaire.QuestionnaireDataRequirementsProvider.class,
                                ca.uhn.fhir.cr.dstu3.valueset.ValueSetDataRequirementsProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

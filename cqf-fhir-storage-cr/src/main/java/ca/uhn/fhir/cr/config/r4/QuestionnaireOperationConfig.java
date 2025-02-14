package ca.uhn.fhir.cr.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.IQuestionnaireProcessorFactory;
import ca.uhn.fhir.cr.config.ProviderLoader;
import ca.uhn.fhir.cr.config.ProviderSelector;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

public class QuestionnaireOperationConfig {
    @Bean
    ca.uhn.fhir.cr.r4.structuredefinition.StructureDefinitionQuestionnaireProvider
            r4StructureDefinitionQuestionnaireProvider(IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        return new ca.uhn.fhir.cr.r4.structuredefinition.StructureDefinitionQuestionnaireProvider(
                questionnaireProcessorFactory);
    }

    @Bean(name = "questionnaireOperationLoader")
    public ProviderLoader questionnaireOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R4,
                        Arrays.asList(
                                ca.uhn.fhir.cr.r4.structuredefinition.StructureDefinitionQuestionnaireProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

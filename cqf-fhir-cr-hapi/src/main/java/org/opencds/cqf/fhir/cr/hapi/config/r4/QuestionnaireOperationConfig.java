package org.opencds.cqf.fhir.cr.hapi.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;

import org.opencds.cqf.fhir.cr.hapi.r4.structuredefinition.StructureDefinitionQuestionnaireProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

public class QuestionnaireOperationConfig {
    @Bean
    StructureDefinitionQuestionnaireProvider
            r4StructureDefinitionQuestionnaireProvider(IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        return new StructureDefinitionQuestionnaireProvider(
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
                                StructureDefinitionQuestionnaireProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

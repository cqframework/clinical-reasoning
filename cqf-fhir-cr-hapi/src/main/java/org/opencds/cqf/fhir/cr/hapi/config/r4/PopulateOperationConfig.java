package org.opencds.cqf.fhir.cr.hapi.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;

import org.opencds.cqf.fhir.cr.hapi.r4.questionnaire.QuestionnairePopulateProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class PopulateOperationConfig {
    @Bean
    QuestionnairePopulateProvider r4QuestionnairePopulateProvider(
            IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        return new QuestionnairePopulateProvider(questionnaireProcessorFactory);
    }

    @Bean(name = "populateOperationLoader")
    public ProviderLoader populateOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R4,
                        Arrays.asList(QuestionnairePopulateProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

package org.opencds.cqf.fhir.cr.hapi.config.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.dstu3.library.LibraryDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.dstu3.plandefinition.PlanDefinitionDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.dstu3.questionnaire.QuestionnaireDataRequirementsProvider;
import org.opencds.cqf.fhir.cr.hapi.dstu3.valueset.ValueSetDataRequirementsProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class DataRequirementsOperationConfig {
    @Bean
    LibraryDataRequirementsProvider dstu3LibraryDataRequirementsProvider(
            ILibraryProcessorFactory libraryProcessorFactory) {
        return new LibraryDataRequirementsProvider(libraryProcessorFactory);
    }

    @Bean
    PlanDefinitionDataRequirementsProvider dstu3PlanDefinitionDataRequirementsProvider(
            IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        return new PlanDefinitionDataRequirementsProvider(planDefinitionProcessorFactory);
    }

    @Bean
    QuestionnaireDataRequirementsProvider dstu3QuestionnaireDataRequirementsProvider(
            IQuestionnaireProcessorFactory questionnaireFactory) {
        return new QuestionnaireDataRequirementsProvider(questionnaireFactory);
    }

    @Bean
    ValueSetDataRequirementsProvider dstu3ValueSetDataRequirementsProvider(IValueSetProcessorFactory valueSetFactory) {
        return new ValueSetDataRequirementsProvider(valueSetFactory);
    }

    @Bean(name = "dataRequirementsOperationLoader")
    public ProviderLoader dataRequirementsOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.DSTU3,
                        Arrays.asList(
                                LibraryDataRequirementsProvider.class,
                                PlanDefinitionDataRequirementsProvider.class,
                                QuestionnaireDataRequirementsProvider.class,
                                ValueSetDataRequirementsProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

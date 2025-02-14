package ca.uhn.fhir.cr.config.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.IActivityDefinitionProcessorFactory;
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
public class ApplyOperationConfig {
    @Bean
    ca.uhn.fhir.cr.dstu3.activitydefinition.ActivityDefinitionApplyProvider dstu3ActivityDefinitionApplyProvider(
            IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory) {
        return new ca.uhn.fhir.cr.dstu3.activitydefinition.ActivityDefinitionApplyProvider(
                activityDefinitionProcessorFactory);
    }

    @Bean
    ca.uhn.fhir.cr.dstu3.plandefinition.PlanDefinitionApplyProvider dstu3PlanDefinitionApplyProvider() {
        return new ca.uhn.fhir.cr.dstu3.plandefinition.PlanDefinitionApplyProvider();
    }

    @Bean(name = "applyOperationLoader")
    public ProviderLoader applyOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.DSTU3,
                        Arrays.asList(
                                ca.uhn.fhir.cr.dstu3.activitydefinition.ActivityDefinitionApplyProvider.class,
                                ca.uhn.fhir.cr.dstu3.plandefinition.PlanDefinitionApplyProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

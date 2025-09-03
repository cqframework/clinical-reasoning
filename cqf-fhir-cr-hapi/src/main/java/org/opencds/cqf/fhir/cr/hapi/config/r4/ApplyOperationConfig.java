package org.opencds.cqf.fhir.cr.hapi.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.IActivityDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionApplyRequestBuilderFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r4.activitydefinition.ActivityDefinitionApplyProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.graphdefinition.GraphDefinitionApplyProvider;
import org.opencds.cqf.fhir.cr.hapi.r4.plandefinition.PlanDefinitionApplyProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class ApplyOperationConfig {
    @Bean
    ActivityDefinitionApplyProvider r4ActivityDefinitionApplyProvider(
        IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory) {
        return new ActivityDefinitionApplyProvider(activityDefinitionProcessorFactory);
    }

    @Bean
    PlanDefinitionApplyProvider r4PlanDefinitionApplyProvider(
        IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        return new PlanDefinitionApplyProvider(planDefinitionProcessorFactory);
    }

    @Bean
    GraphDefinitionApplyProvider r4GraphDefinitionApplyProvider(
        IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory, IGraphDefinitionApplyRequestBuilderFactory graphDefinitionApplyRequestBuilderFactory, FhirContext fhirContext, StringTimePeriodHandler stringTimePeriodHandler) {
        return new GraphDefinitionApplyProvider(
            graphDefinitionProcessorFactory,
            graphDefinitionApplyRequestBuilderFactory,
            fhirContext.getVersion().getVersion(),
            stringTimePeriodHandler);
    }

    @Bean(name = "applyOperationLoader")
    public ProviderLoader applyOperationLoader(
        ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
            fhirContext,
            Map.of(
                fhirContext.getVersion().getVersion(),
                Arrays.asList(
                    ActivityDefinitionApplyProvider.class,
                    PlanDefinitionApplyProvider.class,
                    GraphDefinitionApplyProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

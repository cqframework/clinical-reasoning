package org.opencds.cqf.fhir.cr.hapi.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.List;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.config.RepositoryConfig;
import org.opencds.cqf.fhir.cr.hapi.r4.ISubmitDataProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.r4.ra.SubmitDataProvider;
import org.opencds.cqf.fhir.cr.measure.r4.R4SubmitDataService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RepositoryConfig.class})
public class RiskAdjustmentOperationConfig {

    @Bean
    ISubmitDataProcessorFactory r4SubmitDataProcessorFactory(IRepositoryFactory repositoryFactory) {
        return rd -> new R4SubmitDataService(repositoryFactory.create(rd));
    }

    @Bean(name = "riskAdjustmentSubmitDataProvider")
    SubmitDataProvider r4RaSubmitDataProvider(ISubmitDataProcessorFactory r4SubmitDataProcessorFactory) {
        return new SubmitDataProvider(r4SubmitDataProcessorFactory);
    }

    @Bean(name = "riskAdjustmentOperationLoader")
    public ProviderLoader riskAdjustmentOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {

        var selector = new ProviderSelector(fhirContext, Map.of(FhirVersionEnum.R4, List.of(SubmitDataProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

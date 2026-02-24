package org.opencds.cqf.fhir.cr.hapi.config.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.List;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.config.RepositoryConfig;
import org.opencds.cqf.fhir.cr.hapi.dstu3.IMeasureServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.dstu3.measure.MeasureOperationsProvider;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.dstu3.Dstu3MeasureService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SuppressWarnings("UnstableApiUsage")
@Configuration
@Import({RepositoryConfig.class, CqlOperationConfig.class, EvaluateOperationConfig.class})
public class CrDstu3Config {

    @Bean
    IMeasureServiceFactory dstu3MeasureServiceFactory(
            IRepositoryFactory repositoryFactory, MeasureEvaluationOptions evaluationOptions) {
        return rd -> new Dstu3MeasureService(repositoryFactory.create(rd), evaluationOptions);
    }

    @Bean
    MeasureOperationsProvider dstu3MeasureOperationsProvider(IMeasureServiceFactory dstu3MeasureProcessorFactory) {
        return new MeasureOperationsProvider(dstu3MeasureProcessorFactory);
    }

    @Bean
    public ProviderLoader dstu3PdLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {

        var selector = new ProviderSelector(
                fhirContext, Map.of(FhirVersionEnum.DSTU3, List.of((MeasureOperationsProvider.class))));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

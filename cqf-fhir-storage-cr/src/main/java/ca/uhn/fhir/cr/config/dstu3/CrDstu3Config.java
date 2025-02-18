package ca.uhn.fhir.cr.config.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.IRepositoryFactory;
import ca.uhn.fhir.cr.config.CrBaseConfig;
import ca.uhn.fhir.cr.config.ProviderLoader;
import ca.uhn.fhir.cr.config.ProviderSelector;
import ca.uhn.fhir.cr.config.RepositoryConfig;
import ca.uhn.fhir.cr.dstu3.IMeasureServiceFactory;
import ca.uhn.fhir.cr.dstu3.measure.MeasureOperationsProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.dstu3.Dstu3MeasureService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RepositoryConfig.class, CrBaseConfig.class})
public class CrDstu3Config {

    @Bean
    IMeasureServiceFactory dstu3MeasureServiceFactory(
            IRepositoryFactory repositoryFactory, MeasureEvaluationOptions pvaluationOptions) {
        return rd -> new Dstu3MeasureService(repositoryFactory.create(rd), pvaluationOptions);
    }

    @Bean
    MeasureOperationsProvider dstu3MeasureOperationsProvider(IMeasureServiceFactory dstu3MeasureProcessorFactory) {
        return new MeasureOperationsProvider(dstu3MeasureProcessorFactory);
    }

    @Bean
    public ProviderLoader dstu3PdLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {

        var selector = new ProviderSelector(
                fhirContext, Map.of(FhirVersionEnum.DSTU3, Arrays.asList((MeasureOperationsProvider.class))));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

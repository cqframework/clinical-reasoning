package ca.uhn.fhir.cr.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.ILibraryProcessorFactory;
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
public class EvaluateOperationConfig {
    @Bean
    ca.uhn.fhir.cr.r4.library.LibraryEvaluateProvider r4LibraryEvaluateProvider(
            ILibraryProcessorFactory libraryProcessorFactory) {
        return new ca.uhn.fhir.cr.r4.library.LibraryEvaluateProvider(libraryProcessorFactory);
    }

    @Bean(name = "evaluateOperationLoader")
    public ProviderLoader evaluateOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(FhirVersionEnum.R4, Arrays.asList(ca.uhn.fhir.cr.r4.library.LibraryEvaluateProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

package org.opencds.cqf.fhir.cr.hapi.config.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r5.library.LibraryWithdrawProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WithdrawOperationConfig {

    @Bean
    LibraryWithdrawProvider r5LibraryWithdrawProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        return new LibraryWithdrawProvider(libraryProcessorFactory);
    }

    @Bean(name = "withdrawOperationLoader")
    public ProviderLoader withdrawOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext, Map.of(FhirVersionEnum.R5, Arrays.asList(LibraryWithdrawProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

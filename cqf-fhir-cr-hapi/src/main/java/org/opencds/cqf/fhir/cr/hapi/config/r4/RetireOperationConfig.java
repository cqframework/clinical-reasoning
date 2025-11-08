package org.opencds.cqf.fhir.cr.hapi.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.List;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryRetireProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class RetireOperationConfig {

    @Bean
    LibraryRetireProvider r4LibraryRetireProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        return new LibraryRetireProvider(libraryProcessorFactory);
    }

    @Bean(name = "retireOperationLoader")
    public ProviderLoader retireOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector =
                new ProviderSelector(fhirContext, Map.of(FhirVersionEnum.R4, List.of(LibraryRetireProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

package org.opencds.cqf.fhir.cr.hapi.config.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.List;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.IRepositoryFactory;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r4.library.LibraryReleaseProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReleaseOperationConfig {
    @Bean
    LibraryReleaseProvider r4LibraryReleaseProvider(IRepositoryFactory repositoryFactory) {
        return new LibraryReleaseProvider(repositoryFactory);
    }

    @Bean(name = "releaseOperationLoader")
    public ProviderLoader releaseOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector =
                new ProviderSelector(fhirContext, Map.of(FhirVersionEnum.R4, List.of(LibraryReleaseProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

package org.opencds.cqf.fhir.cr.hapi.config.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.Arrays;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r5.library.LibraryReviseProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

public class ReviseOperationConfig {

    @Bean
    LibraryReviseProvider r5LibraryReviseProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        return new LibraryReviseProvider(libraryProcessorFactory);
    }

    @Bean(name = "reviseOperationLoader")
    public ProviderLoader reviseOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext, Map.of(FhirVersionEnum.R5, Arrays.asList(LibraryReviseProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

package org.opencds.cqf.fhir.cr.hapi.config.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.List;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.IImplementationGuideProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.r5.implementationguide.ImplementationGuideReleaseProvider;
import org.opencds.cqf.fhir.cr.hapi.r5.library.LibraryReleaseProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class ReleaseOperationConfig {
    @Bean
    ImplementationGuideReleaseProvider r5ImplementationGuideReleaseProvider(
            IImplementationGuideProcessorFactory implementationGuideProcessorFactory) {
        return new ImplementationGuideReleaseProvider(implementationGuideProcessorFactory);
    }

    @Bean
    LibraryReleaseProvider r5LibraryReleaseProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        return new LibraryReleaseProvider(libraryProcessorFactory);
    }

    @Bean(name = "releaseOperationLoader")
    public ProviderLoader releaseOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext,
                Map.of(
                        FhirVersionEnum.R5,
                        List.of(ImplementationGuideReleaseProvider.class, LibraryReleaseProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

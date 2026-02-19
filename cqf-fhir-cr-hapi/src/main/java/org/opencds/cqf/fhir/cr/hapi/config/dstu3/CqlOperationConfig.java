package org.opencds.cqf.fhir.cr.hapi.config.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.List;
import java.util.Map;
import org.opencds.cqf.fhir.cr.hapi.common.ICqlProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderLoader;
import org.opencds.cqf.fhir.cr.hapi.config.ProviderSelector;
import org.opencds.cqf.fhir.cr.hapi.dstu3.cql.CqlExecutionOperationProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrProcessorConfig.class)
public class CqlOperationConfig {
    @Bean
    CqlExecutionOperationProvider dstu3CqlExecutionOperationProvider(ICqlProcessorFactory cqlProcessorFactory) {
        return new CqlExecutionOperationProvider(cqlProcessorFactory);
    }

    @Bean(name = "cqlOperationLoader")
    public ProviderLoader cqlOperationLoader(
            ApplicationContext applicationContext, FhirContext fhirContext, RestfulServer restfulServer) {
        var selector = new ProviderSelector(
                fhirContext, Map.of(FhirVersionEnum.DSTU3, List.of(CqlExecutionOperationProvider.class)));

        return new ProviderLoader(restfulServer, applicationContext, selector);
    }
}

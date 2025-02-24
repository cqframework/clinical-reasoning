package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.opencds.cqf.fhir.cr.hapi.common.IRepositoryFactory;
import org.opencds.cqf.fhir.cr.hapi.common.RepositoryFactoryForRepositoryInterface;
import org.opencds.cqf.fhir.cr.hapi.repo.HapiFhirRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    @Bean
    IRepositoryFactory repositoryFactory(DaoRegistry daoRegistry, RestfulServer restfulServer) {
        return rd -> new HapiFhirRepository(daoRegistry, rd, restfulServer);
    }

    @Bean
    RepositoryFactoryForRepositoryInterface repositoryFactoryForInterface(
            DaoRegistry daoRegistry, RestfulServer restfulServer) {
        return rd -> new HapiFhirRepository(daoRegistry, rd, restfulServer);
    }
}

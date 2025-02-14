package ca.uhn.fhir.cr.config;

import ca.uhn.fhir.cr.common.IRepositoryFactory;
import ca.uhn.fhir.cr.common.RepositoryFactoryForRepositoryInterface;
import ca.uhn.fhir.cr.repo.HapiFhirRepository;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.server.RestfulServer;
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

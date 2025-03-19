package org.opencds.cqf.fhir.cr.hapi.config.test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cr.common.IRepositoryFactory;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.searchparam.MatchUrlService;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsConfigService;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsHooksDaoAuthorizationSvc;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsServiceRegistry;
import ca.uhn.hapi.fhir.cdshooks.module.CdsHooksObjectMapperFactory;
import ca.uhn.hapi.fhir.cdshooks.serializer.CdsServiceRequestJsonDeserializer;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsConfigServiceImpl;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsHooksContextBooter;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsServiceRegistryImpl;
import ca.uhn.hapi.fhir.cdshooks.svc.cr.CdsCrSettings;
import ca.uhn.hapi.fhir.cdshooks.svc.cr.ICdsCrService;
import ca.uhn.hapi.fhir.cdshooks.svc.cr.ICdsCrServiceFactory;
import ca.uhn.hapi.fhir.cdshooks.svc.cr.discovery.ICrDiscoveryService;
import ca.uhn.hapi.fhir.cdshooks.svc.cr.discovery.ICrDiscoveryServiceFactory;
import ca.uhn.hapi.fhir.cdshooks.svc.prefetch.CdsPrefetchDaoSvc;
import ca.uhn.hapi.fhir.cdshooks.svc.prefetch.CdsPrefetchFhirClientSvc;
import ca.uhn.hapi.fhir.cdshooks.svc.prefetch.CdsPrefetchSvc;
import ca.uhn.hapi.fhir.cdshooks.svc.prefetch.CdsResolutionStrategySvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// This is all copied into jpaserver-starter so that plain ole cds-hooks still works.
// Then we deprecate all the cr-specific stuff in hapi
// and remove the cr-specific config in the next release.
@Configuration
@SuppressWarnings({"squid:S125", "squid:S1135", "squid:S6548"})
public class TestCdsHooksConfig {

    public static final String CDS_HOOKS_OBJECT_MAPPER_FACTORY = "cdsHooksObjectMapperFactory";

    private final DaoRegistry daoRegistry;

    // TODO: LD: once we're using a new version of hapi-fhir, get rid of this, the import, and all
    // code that uses or assigns to it:
    private final IRepositoryFactory repositoryFactory;

    private final MatchUrlService matchUrlService;

    private final RestfulServer restfulServer;

    public TestCdsHooksConfig(
            Optional<DaoRegistry> daoRegistry,
            Optional<IRepositoryFactory> repositoryFactory,
            Optional<MatchUrlService> matchUrlService,
            Optional<RestfulServer> restfulServer) {
        this.daoRegistry = daoRegistry.orElse(null);
        this.repositoryFactory = repositoryFactory.orElse(null);
        this.matchUrlService = matchUrlService.orElse(null);
        this.restfulServer = restfulServer.orElse(null);
    }

    @Bean(name = CDS_HOOKS_OBJECT_MAPPER_FACTORY)
    public ObjectMapper objectMapper(FhirContext fhirContext) {
        return new CdsHooksObjectMapperFactory(fhirContext).newMapper();
    }

    @Bean
    public ICdsServiceRegistry cdsServiceRegistry(
            CdsHooksContextBooter cdsHooksContextBooter,
            CdsPrefetchSvc cdsPrefetchSvc,
            @Qualifier(CDS_HOOKS_OBJECT_MAPPER_FACTORY) ObjectMapper objectMapper,
            FhirContext fhirContext) {
        final CdsServiceRequestJsonDeserializer cdsServiceRequestJsonDeserializer =
                new CdsServiceRequestJsonDeserializer(fhirContext, objectMapper);

        return new CdsServiceRegistryImpl(
                cdsHooksContextBooter,
                cdsPrefetchSvc,
                objectMapper,
                StubbedICdsCrServiceFactory.INSTANCE,
                StubbedICrDiscoveryServiceFactory.INSTANCE,
                cdsServiceRequestJsonDeserializer);
        // TODO: LD:  once we upgrade to a new version of hapi-fhir with CDS CR removed, replace
        // the line above with the line below
        //        return new CdsServiceRegistryImpl(cdsHooksContextBooter, cdsPrefetchSvc, objectMapper,
        // cdsServiceRequestJsonDeserializer);
    }

    @Bean
    public ICdsConfigService cdsConfigService(
            FhirContext fhirContext, @Qualifier(CDS_HOOKS_OBJECT_MAPPER_FACTORY) ObjectMapper objectMapper) {
        return new CdsConfigServiceImpl(
                fhirContext, objectMapper, CdsCrSettings.getDefault(), daoRegistry, repositoryFactory, restfulServer);
        // TODO: LD:  once we upgrade to a new version of hapi-fhir with CDS CR removed, replace
        // the line above with the line below
        //        return new CdsConfigServiceImpl(fhirContext, objectMapper, daoRegistry, restfulServer);
    }

    @Bean
    CdsPrefetchSvc cdsPrefetchSvc(
            CdsResolutionStrategySvc cdsResolutionStrategySvc,
            CdsPrefetchDaoSvc resourcePrefetchDao,
            CdsPrefetchFhirClientSvc resourcePrefetchFhirClient,
            ICdsHooksDaoAuthorizationSvc cdsHooksDaoAuthorizationSvc,
            @Nullable IInterceptorBroadcaster interceptorBroadcaster) {
        return new CdsPrefetchSvc(
                cdsResolutionStrategySvc,
                resourcePrefetchDao,
                resourcePrefetchFhirClient,
                cdsHooksDaoAuthorizationSvc,
                interceptorBroadcaster);
    }

    @Bean
    CdsPrefetchDaoSvc resourcePrefetchDao(DaoRegistry daoRegistry, FhirContext fhirContext) {
        return new CdsPrefetchDaoSvc(daoRegistry, matchUrlService, fhirContext);
    }

    @Bean
    CdsPrefetchFhirClientSvc resourcePrefetchFhirClient(FhirContext fhirContext) {
        return new CdsPrefetchFhirClientSvc(fhirContext);
    }

    @Bean
    CdsResolutionStrategySvc cdsResolutionStrategySvc() {
        return new CdsResolutionStrategySvc(daoRegistry);
    }

    // TODO: LD: this is code for a temporary stub: delete when no longer needed above
    enum StubbedICdsCrServiceFactory implements ICdsCrServiceFactory {
        INSTANCE;

        @Override
        public ICdsCrService create(String serviceId) {
            return null;
        }
    }

    // TODO: LD: this is code for a temporary stub: delete when no longer needed above
    enum StubbedICrDiscoveryServiceFactory implements ICrDiscoveryServiceFactory {
        INSTANCE;

        @Override
        public ICrDiscoveryService create(String serviceId) {
            return null;
        }
    }
}

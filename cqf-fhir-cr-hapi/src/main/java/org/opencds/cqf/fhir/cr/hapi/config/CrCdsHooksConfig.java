package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.cache.IResourceChangeListenerRegistry;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRestfulResponse;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsConfigService;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsServiceRegistryImpl;
import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrServiceRegistry;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceInterceptor;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.ICdsCrServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.ICdsCrServiceRegistry;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.CdsCrDiscoveryServiceRegistry;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.ICdsCrDiscoveryServiceRegistry;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.ICrDiscoveryServiceFactory;
import org.opencds.cqf.fhir.utility.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SuppressWarnings("UnstableApiUsage")
@Configuration
@Import({CrCdsConfig.class})
public class CrCdsHooksConfig {
    private static final Logger ourLog = LoggerFactory.getLogger(CrCdsHooksConfig.class);

    public static final String PLAN_DEFINITION_RESOURCE_NAME = "PlanDefinition";

    @Nullable
    private final IRepositoryFactory repositoryFactory;

    public CrCdsHooksConfig(Optional<IRepositoryFactory> repositoryFactory) {
        this.repositoryFactory = repositoryFactory.orElse(null);
    }

    private RequestDetails createRequestDetails(FhirContext fhirContext, RestfulServer restfulServer, String id) {
        var rd = new SystemRequestDetails();
        rd.setServer(restfulServer);
        rd.setResponse(new SystemRestfulResponse(rd));
        rd.setId(Ids.newId(fhirContext.getVersion().getVersion(), CrCdsHooksConfig.PLAN_DEFINITION_RESOURCE_NAME, id));
        return rd;
    }

    @Bean
    public ICdsCrServiceRegistry cdsCrServiceRegistry() {
        return new CdsCrServiceRegistry();
    }

    @Bean
    public ICdsCrServiceFactory cdsCrServiceFactory(
            FhirContext fhirContext, ICdsConfigService cdsConfigService, ICdsCrServiceRegistry cdsCrServiceRegistry) {
        return id -> {
            if (repositoryFactory == null) {
                return null;
            }
            var rd = createRequestDetails(fhirContext, cdsConfigService.getRestfulServer(), id);
            var repository = repositoryFactory.create(rd);
            var clazz = cdsCrServiceRegistry.find(fhirContext.getVersion().getVersion());
            if (clazz.isEmpty()) {
                return null;
            }
            try {
                var constructor = clazz.get().getConstructor(RequestDetails.class, IRepository.class);
                return constructor.newInstance(rd, repository);
            } catch (NoSuchMethodException
                    | InvocationTargetException
                    | InstantiationException
                    | IllegalAccessException e) {
                ourLog.error(
                        "Error encountered attempting to construct the CdsCrService: %s".formatted(e.getMessage()));
                return null;
            }
        };
    }

    @Bean
    public ICdsCrDiscoveryServiceRegistry cdsCrDiscoveryServiceRegistry() {
        return new CdsCrDiscoveryServiceRegistry();
    }

    @Bean
    public ICrDiscoveryServiceFactory crDiscoveryServiceFactory(
            FhirContext fhirContext,
            ICdsConfigService cdsConfigService,
            ICdsCrDiscoveryServiceRegistry cdsCrDiscoveryServiceRegistry) {
        return id -> {
            if (repositoryFactory == null) {
                return null;
            }
            var rd = createRequestDetails(fhirContext, cdsConfigService.getRestfulServer(), id);
            var repository = repositoryFactory.create(rd);
            var clazz =
                    cdsCrDiscoveryServiceRegistry.find(fhirContext.getVersion().getVersion());
            if (clazz.isEmpty()) {
                return null;
            }
            try {
                var constructor = clazz.get().getConstructor(IIdType.class, IRepository.class);
                return constructor.newInstance(rd.getId(), repository);
            } catch (NoSuchMethodException
                    | InvocationTargetException
                    | InstantiationException
                    | IllegalAccessException e) {
                ourLog.error("Error encountered attempting to construct the CrDiscoveryService: %s"
                        .formatted(e.getMessage()));
                return null;
            }
        };
    }

    @Bean
    public CdsServiceInterceptor cdsServiceInterceptor(
            CdsServiceRegistryImpl cdsServiceRegistry,
            ICrDiscoveryServiceFactory discoveryServiceFactory,
            ICdsCrServiceFactory crServiceFactory,
            PartitionSettings partitionSettings,
            Optional<IResourceChangeListenerRegistry> resourceChangeListenerRegistry) {
        if (resourceChangeListenerRegistry.isEmpty()) {
            return null;
        }
        var listener = new CdsServiceInterceptor(cdsServiceRegistry, discoveryServiceFactory, crServiceFactory);
        resourceChangeListenerRegistry
                .get()
                .registerResourceResourceChangeListener(
                        PLAN_DEFINITION_RESOURCE_NAME,
                        RequestPartitionId.defaultPartition(partitionSettings),
                        SearchParameterMap.newSynchronous(),
                        listener,
                        1000);
        return listener;
    }
}

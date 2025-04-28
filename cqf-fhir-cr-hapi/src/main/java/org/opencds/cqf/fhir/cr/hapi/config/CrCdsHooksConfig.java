package org.opencds.cqf.fhir.cr.hapi.config;

import static ca.uhn.hapi.fhir.cdshooks.config.CdsHooksConfig.CDS_HOOKS_OBJECT_MAPPER_FACTORY;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.cache.IResourceChangeListenerRegistry;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsConfigService;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsServiceRegistryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrServiceRegistry;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsServiceInterceptor;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.ICdsCrService;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.ICdsCrServiceFactory;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.ICdsCrServiceRegistry;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.CdsCrDiscoveryServiceRegistry;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.ICdsCrDiscoveryServiceRegistry;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.ICrDiscoveryService;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.ICrDiscoveryServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CrCdsConfig.class)
public class CrCdsHooksConfig {
    private static final Logger ourLog = LoggerFactory.getLogger(CrCdsHooksConfig.class);

    public static final String PLAN_DEFINITION_RESOURCE_NAME = "PlanDefinition";

    @Nullable
    private final IRepositoryFactory repositoryFactory;

    public CrCdsHooksConfig(Optional<IRepositoryFactory> repositoryFactory) {
        this.repositoryFactory = repositoryFactory.orElse(null);
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
            RequestDetails rd = cdsConfigService.createRequestDetails(fhirContext, id, PLAN_DEFINITION_RESOURCE_NAME);
            IRepository repository = repositoryFactory.create(rd);
            Optional<Class<? extends ICdsCrService>> clazz =
                    cdsCrServiceRegistry.find(fhirContext.getVersion().getVersion());
            if (clazz.isEmpty()) {
                return null;
            }
            try {
                Constructor<? extends ICdsCrService> constructor =
                        clazz.get().getConstructor(RequestDetails.class, IRepository.class, ICdsConfigService.class);
                return constructor.newInstance(rd, repository, cdsConfigService);
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
            RequestDetails rd = cdsConfigService.createRequestDetails(fhirContext, id, PLAN_DEFINITION_RESOURCE_NAME);
            IRepository repository = repositoryFactory.create(rd);
            Optional<Class<? extends ICrDiscoveryService>> clazz =
                    cdsCrDiscoveryServiceRegistry.find(fhirContext.getVersion().getVersion());
            if (clazz.isEmpty()) {
                return null;
            }
            try {
                Constructor<? extends ICrDiscoveryService> constructor =
                        clazz.get().getConstructor(IIdType.class, IRepository.class);
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
            @Qualifier(CDS_HOOKS_OBJECT_MAPPER_FACTORY) ObjectMapper om,
            Optional<IResourceChangeListenerRegistry> resourceChangeListenerRegistry) {
        if (resourceChangeListenerRegistry.isEmpty()) {
            return null;
        }
        CdsServiceInterceptor listener =
                new CdsServiceInterceptor(cdsServiceRegistry, discoveryServiceFactory, crServiceFactory, om);
        resourceChangeListenerRegistry
                .get()
                .registerResourceResourceChangeListener(
                        PLAN_DEFINITION_RESOURCE_NAME, SearchParameterMap.newSynchronous(), listener, 1000);
        return listener;
    }
}

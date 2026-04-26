package org.opencds.cqf.fhir.cr.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * On Spring context refresh, registers one {@link RepositoryResourceProvider} per FHIR resource
 * type with the supplied {@link RestfulServer}, plus a single {@link RepositorySystemProvider}
 * for system-level interactions ({@code POST /} bundle transactions).
 *
 * <p>If {@code resourceTypes} is null, every concrete resource type known to the
 * {@link FhirContext} is registered. Pass an explicit list to scope the surface (e.g. expose
 * only knowledge artifacts).
 */
public class RepositoryRestProviderRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryRestProviderRegistrar.class);

    /**
     * Abstract / infrastructure types that are not concrete resources but may appear in
     * {@link FhirContext#getResourceTypes()} on some HAPI versions.
     */
    private static final Set<String> NON_REGISTERABLE = Set.of("Resource", "DomainResource");

    private final RestfulServer restfulServer;
    private final IRepositoryFactory repositoryFactory;
    private final FhirContext fhirContext;
    private final List<String> resourceTypes;

    public RepositoryRestProviderRegistrar(
            RestfulServer restfulServer,
            IRepositoryFactory repositoryFactory,
            FhirContext fhirContext,
            List<String> resourceTypes) {
        this.restfulServer = restfulServer;
        this.repositoryFactory = repositoryFactory;
        this.fhirContext = fhirContext;
        this.resourceTypes = resourceTypes;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void registerProviders() {
        var types = resourceTypes != null ? resourceTypes : allConcreteResourceTypes();
        for (String typeName : types) {
            registerOne(typeName);
        }
        restfulServer.registerProvider(new RepositorySystemProvider(repositoryFactory));
        logger.info(
                "Registered {} IRepository-backed CRUD providers + system provider",
                types.size());
    }

    private List<String> allConcreteResourceTypes() {
        return fhirContext.getResourceTypes().stream()
                .filter(t -> !NON_REGISTERABLE.contains(t))
                .sorted()
                .toList();
    }

    private <T extends IBaseResource> void registerOne(String typeName) {
        var def = fhirContext.getResourceDefinition(typeName);
        @SuppressWarnings("unchecked")
        Class<T> implClass = (Class<T>) def.getImplementingClass();
        var provider = new RepositoryResourceProvider<>(implClass, repositoryFactory, fhirContext);
        restfulServer.registerProvider(provider);
    }
}

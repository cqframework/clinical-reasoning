package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BundleBuilder;
import ca.uhn.fhir.util.BundleUtil;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.matcher.BaseResourceMatcher;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherDSTU3;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR4;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR5;

public class InMemoryFhirRepository implements Repository {

    private final Map<IIdType, IBaseResource> resourceMap;
    private final FhirContext context;

    public InMemoryFhirRepository(FhirContext context) {
        this.context = context;
        this.resourceMap = new LinkedHashMap<>();
    }

    public InMemoryFhirRepository(FhirContext context, Class<?> clazz, List<String> directoryList, boolean recursive) {
        this.context = context;
        // TODO: Resource loader.
        this.resourceMap = new LinkedHashMap<>();
        // var resourceLoader = new FhirResourceLoader(context, clazz, directoryList, recursive);
        // this.resourceMap = Maps.uniqueIndex(resourceLoader.getResources(),
        // r -> Ids.newId(this.context, r.getIdElement().getResourceType(),
        // r.getIdElement().getIdPart()));
    }

    public InMemoryFhirRepository(FhirContext context, IBaseBundle bundle) {
        this.context = context;
        this.resourceMap = Maps.uniqueIndex(
                BundleUtil.toListOfResources(this.context, bundle),
                r -> Ids.newId(
                        this.context,
                        r.getIdElement().getResourceType(),
                        r.getIdElement().getIdPart()));
    }

    public BaseResourceMatcher getResourceMatcher() {
        var fhirVersion = context.getVersion().getVersion();
        switch (fhirVersion) {
            case DSTU3:
                return new ResourceMatcherDSTU3(FhirModelResolverCache.resolverForVersion(fhirVersion));
            case R4:
                return new ResourceMatcherR4(FhirModelResolverCache.resolverForVersion(fhirVersion));
            case R5:
                return new ResourceMatcherR5(FhirModelResolverCache.resolverForVersion(fhirVersion));
            default:
                throw new NotImplementedException(
                        "Resource matching is not implemented for FHIR version " + fhirVersion.getFhirVersionString());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {
        var theId = Ids.newId(context, resourceType.getSimpleName(), id.getIdPart());
        if (resourceMap.containsKey(theId)) {
            return (T) resourceMap.get(theId);
        }
        throw new ResourceNotFoundException("Resource not found with id " + theId);
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        var outcome = new MethodOutcome();
        var theId = Ids.newRandomId(context, resource.getIdElement().getResourceType());
        while (resourceMap.containsKey(theId)) {
            theId = Ids.newRandomId(context, resource.getIdElement().getResourceType());
        }
        resource.setId(theId);
        resourceMap.put(theId, resource);
        outcome.setCreated(true);
        return outcome;
    }

    @Override
    public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(
            I id, P patchParameters, Map<String, String> headers) {
        throw new NotImplementedException("The PATCH operation is not currently supported");
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        var outcome = new MethodOutcome();
        var theId = Ids.newId(
                context,
                resource.getIdElement().getResourceType(),
                resource.getIdElement().getIdPart());
        if (!resourceMap.containsKey(theId)) {
            outcome.setCreated(true);
        }
        resourceMap.put(theId, resource);
        return outcome;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> resourceType, I id, Map<String, String> headers) {
        var outcome = new MethodOutcome();
        var theId = Ids.newId(context, resourceType.getSimpleName(), id.getIdPart());
        if (resourceMap.containsKey(theId)) {
            resourceMap.remove(theId);
        } else {
            throw new ResourceNotFoundException("Resource not found with id " + theId);
        }
        return outcome;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Map<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        BundleBuilder builder = new BundleBuilder(this.context);

        var resourceList = resourceMap.values().stream()
                .filter(resource -> resource.getIdElement().getResourceType().equals(resourceType.getSimpleName()))
                .collect(Collectors.toList());

        List<IBaseResource> filteredResources = new ArrayList<>();
        if (searchParameters != null && !searchParameters.isEmpty()) {
            var resourceMatcher = getResourceMatcher();
            for (var resource : resourceList) {
                boolean include = false;
                for (var nextEntry : searchParameters.entrySet()) {
                    var paramName = nextEntry.getKey();
                    if (resourceMatcher.matches(paramName, nextEntry.getValue(), resource)) {
                        include = true;
                    } else {
                        include = false;
                        break;
                    }
                }
                if (include) {
                    filteredResources.add(resource);
                }
            }
            filteredResources.forEach(builder::addCollectionEntry);
        } else {
            resourceList.forEach(builder::addCollectionEntry);
        }

        builder.setType("searchset");
        return (B) builder.getBundle();
    }

    @Override
    public <B extends IBaseBundle> B link(Class<B> bundleType, String url, Map<String, String> headers) {
        throw new NotImplementedException("Paging is not currently supported");
    }

    @Override
    public <C extends IBaseConformance> C capabilities(Class<C> resourceType, Map<String, String> headers) {
        throw new NotImplementedException("The capabilities interaction is not currently supported");
    }

    @Override
    public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
        throw new NotImplementedException("The transaction operation is not currently supported");
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(
            String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(
            Class<T> resourceType, String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(
            I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(
            I id, String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedException();
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters> B history(
            P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException("The history interaction is not currently supported");
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(
            Class<T> resourceType, P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException("The history interaction is not currently supported");
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(
            I id, P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedException("The history interaction is not currently supported");
    }

    @Override
    public FhirContext fhirContext() {
        return this.context;
    }
}

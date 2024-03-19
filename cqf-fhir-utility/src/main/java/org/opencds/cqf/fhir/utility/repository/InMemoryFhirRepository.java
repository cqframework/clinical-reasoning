package org.opencds.cqf.fhir.utility.repository;

import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BundleBuilder;
import ca.uhn.fhir.util.BundleUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Ids;

public class InMemoryFhirRepository implements Repository {

    private final Map<String, Map<IIdType, IBaseResource>> resourceMap;
    private final FhirContext context;
    private final Map<String, Function<IBaseParameters, Object>> operationMap;

    public InMemoryFhirRepository(FhirContext context) {
        this.context = context;
        this.resourceMap = new HashMap<>();
        this.operationMap = new HashMap<>();
    }

    // public InMemoryFhirRepository(FhirContext context, Class<?> clazz, List<String> directoryList, boolean recursive)
    // {
    //     this.context = context;
    //     // TODO: Resource loader.
    //     this.resourceMap = new HashMap<>();
    //     this.operationMap = new HashMap<>();
    //     // var resourceLoader = new FhirResourceLoader(context, clazz, directoryList,
    //     // recursive);
    //     // this.resourceMap = Maps.uniqueIndex(resourceLoader.getResources(),
    //     // r -> Ids.newId(this.context, r.fhirType(),
    //     // r.getIdElement().getIdPart()));
    // }

    public InMemoryFhirRepository(FhirContext context, IBaseBundle bundle) {
        this.context = context;
        var resources = BundleUtil.toListOfResources(this.context, bundle);
        this.resourceMap = resources.stream()
                .collect(Collectors.groupingBy(
                        IBaseResource::fhirType,
                        Collectors.toMap(r -> r.getIdElement().toUnqualifiedVersionless(), Function.identity())));
        this.operationMap = new HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {
        var resources = this.resourceMap.computeIfAbsent(resourceType.getSimpleName(), x -> new HashMap<>());

        var resource = resources.get(id.toUnqualifiedVersionless());

        if (resource == null) {
            throw new ResourceNotFoundException(id);
        }

        return (T) resource;
    }

    @Override
    public <T extends IBaseResource> MethodOutcome create(T resource, Map<String, String> headers) {
        var resources = resourceMap.computeIfAbsent(resource.fhirType(), r -> new HashMap<>());
        var theId = Ids.newRandomId(context, resource.fhirType());
        while (resources.containsKey(theId)) {
            theId = Ids.newRandomId(context, resource.fhirType());
        }
        resource.setId(theId);
        var outcome = new MethodOutcome(theId, true);
        resources.put(theId.toUnqualifiedVersionless(), resource);
        return outcome;
    }

    @Override
    public <I extends IIdType, P extends IBaseParameters> MethodOutcome patch(
            I id, P patchParameters, Map<String, String> headers) {
        throw new NotImplementedException("The PATCH operation is not currently supported");
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        var resources = resourceMap.computeIfAbsent(resource.fhirType(), r -> new HashMap<>());
        var theId = resource.getIdElement().toUnqualifiedVersionless();
        var outcome = new MethodOutcome(theId, false);
        if (!resources.containsKey(theId)) {
            outcome.setCreated(true);
        }
        resources.put(theId, resource);
        return outcome;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> resourceType, I id, Map<String, String> headers) {
        var outcome = new MethodOutcome();
        var resources = resourceMap.computeIfAbsent(id.getResourceType(), r -> new HashMap<>());
        var keyId = id.toUnqualifiedVersionless();
        if (resources.containsKey(keyId)) {
            resources.remove(keyId);
        } else {
            throw new ResourceNotFoundException("Resource not found with id " + id);
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
        var resourceIdMap = resourceMap.computeIfAbsent(resourceType.getSimpleName(), r -> new HashMap<>());

        if (searchParameters == null || searchParameters.isEmpty()) {
            resourceIdMap.values().forEach(builder::addCollectionEntry);
            builder.setType("searchset");
            return (B) builder.getBundle();
        }

        Collection<IBaseResource> candidates;
        if (searchParameters.containsKey("_id")) {
            // We are consuming the _id parameter in this if statement
            var idQueries = searchParameters.get("_id");
            searchParameters.remove("_id");

            // The _id param can be a list of ids
            var idResources = new ArrayList<IBaseResource>(idQueries.size());
            for (var idQuery : idQueries) {
                var idToken = (TokenParam) idQuery;
                // Need to construct the equivalent "UnqualifiedVersionless" id that the map is
                // indexed by. If an id has a version it won't match. Need apples-to-apples Ids types
                var id = Ids.newId(context, resourceType.getSimpleName(), idToken.getValue());
                var r = resourceIdMap.get(id);
                if (r != null) {
                    idResources.add(r);
                }
            }

            candidates = idResources;
        } else {
            candidates = resourceIdMap.values();
        }

        // Apply the rest of the filters
        var resourceMatcher = Repositories.getResourceMatcher(this.context);
        for (var resource : candidates) {
            boolean include = true;
            for (var nextEntry : searchParameters.entrySet()) {
                var paramName = nextEntry.getKey();
                if (!resourceMatcher.matches(paramName, nextEntry.getValue(), resource)) {
                    include = false;
                    break;
                }
            }

            if (include) {
                builder.addCollectionEntry(resource);
            }
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

    @SuppressWarnings("unchecked")
    public static <B extends IBaseBundle> B transactionStub(B transaction, Repository repository) {
        var version = transaction.getStructureFhirVersionEnum();
        var returnBundle = (B) newBundle(version);
        BundleHelper.getEntry(transaction).forEach(e -> {
            if (BundleHelper.isEntryRequestPut(version, e)) {
                var outcome = repository.update(BundleHelper.getEntryResource(version, e));
                var location = outcome.getId().getValue();
                BundleHelper.addEntry(
                        returnBundle,
                        BundleHelper.newEntryWithResponse(
                                version, BundleHelper.newResponseWithLocation(version, location)));
            } else if (BundleHelper.isEntryRequestPost(version, e)) {
                var outcome = repository.create(BundleHelper.getEntryResource(version, e));
                var location = outcome.getId().getValue();
                BundleHelper.addEntry(
                        returnBundle,
                        BundleHelper.newEntryWithResponse(
                                version, BundleHelper.newResponseWithLocation(version, location)));
            } else {
                throw new NotImplementedOperationException("Transaction stub only supports PUT or POST");
            }
        });
        return returnBundle;
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
        return invokeOperation(name, parameters);
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

    @SuppressWarnings("unchecked")
    protected <R extends Object> R invokeOperation(String operationName, IBaseParameters parameters) {
        return (R) operationMap.get(operationName).apply(parameters);
    }
}

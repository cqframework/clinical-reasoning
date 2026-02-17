package org.opencds.cqf.fhir.utility.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BundleBuilder;
import ca.uhn.fhir.util.BundleUtil;
import com.google.common.collect.Multimap;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher;
import org.opencds.cqf.fhir.utility.operation.OperationRegistry;

@SuppressWarnings("UnstableApiUsage")
public class InMemoryFhirRepository implements IRepository {

    private final Map<String, Map<IIdType, IBaseResource>> resourceMap;
    private final FhirContext context;
    private final OperationRegistry operationRegistry;
    private final ResourceMatcher resourceMatcher;

    public InMemoryFhirRepository(FhirContext context) {
        this.context = context;
        this.resourceMap = new HashMap<>();
        this.operationRegistry = new OperationRegistry();
        this.resourceMatcher = Repositories.getResourceMatcher(this.context);
    }

    public InMemoryFhirRepository(FhirContext context, IBaseBundle bundle) {
        this.context = context;
        var resources = BundleUtil.toListOfResources(this.context, bundle);
        this.resourceMap = resources.stream()
                .collect(Collectors.groupingBy(
                        IBaseResource::fhirType,
                        Collectors.toMap(r -> r.getIdElement().toUnqualifiedVersionless(), Function.identity())));
        this.operationRegistry = new OperationRegistry();
        this.resourceMatcher = Repositories.getResourceMatcher(this.context);
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
        throw new NotImplementedOperationException("The PATCH operation is not currently supported");
    }

    @Override
    public <T extends IBaseResource> MethodOutcome update(T resource, Map<String, String> headers) {
        var resources = resourceMap.computeIfAbsent(resource.fhirType(), r -> new HashMap<>());
        var theId = resource.getIdElement().toUnqualifiedVersionless();
        var outcome = new MethodOutcome(theId, false);
        if (!resources.containsKey(theId)) {
            outcome.setCreated(true);
        }
        if (resource.fhirType().equals("SearchParameter")) {
            this.resourceMatcher.addCustomParameter(BundleHelper.resourceToRuntimeSearchParam(resource));
        }
        resources.put(theId, resource);

        return outcome;
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> MethodOutcome delete(
            Class<T> resourceType, I id, Map<String, String> headers) {
        var outcome = new MethodOutcome(id, false);
        var resources = resourceMap.computeIfAbsent(id.getResourceType(), r -> new HashMap<>());
        var keyId = id.toUnqualifiedVersionless();
        if (resources.containsKey(keyId)) {
            outcome.setResource(resources.get(keyId));
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
            Multimap<String, List<IQueryParameterType>> searchParameters,
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

            // The _id param can be a list of ids
            var idResources = new ArrayList<IBaseResource>(idQueries.size());
            for (var idQuery : idQueries) {
                assert idQuery != null;
                for (var query : idQuery) {
                    if (query instanceof TokenParam idToken) {
                        // Need to construct the equivalent "UnqualifiedVersionless" id that the map is
                        // indexed by. If an id has a version it won't match. Need apples-to-apples Ids types
                        var id = Ids.newId(context, resourceType.getSimpleName(), idToken.getValue());
                        var r = resourceIdMap.get(id);
                        if (r != null) {
                            idResources.add(r);
                        }
                    }
                }
            }

            candidates = idResources;
            searchParameters.removeAll("_id");
        } else {
            candidates = resourceIdMap.values();
        }

        // Apply the rest of the filters
        var keyset = searchParameters.keySet();
        for (var resource : candidates) {
            boolean include = true;
            for (String paramName : keyset) {
                /*
                 * each individual entry in a list is 'or'd'.
                 * All the lists from each
                 */
                Collection<List<IQueryParameterType>> values = searchParameters.get(paramName);
                boolean matches = false;
                for (List<IQueryParameterType> ors : values) {
                    matches = this.resourceMatcher.matches(paramName, ors, resource);
                    if (matches) {
                        break;
                    }
                }
                include = matches;

                if (!include) {
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
        throw new NotImplementedOperationException("Paging is not currently supported");
    }

    @Override
    public <C extends IBaseConformance> C capabilities(Class<C> resourceType, Map<String, String> headers) {
        throw new NotImplementedOperationException("The capabilities interaction is not currently supported");
    }

    @Override
    public <B extends IBaseBundle> B transaction(B transaction, Map<String, String> headers) {
        var version = transaction.getStructureFhirVersionEnum();

        @SuppressWarnings("unchecked")
        var returnBundle = (B) newBundle(version);
        BundleHelper.getEntry(transaction).forEach(e -> {
            if (BundleHelper.isEntryRequestPut(version, e)) {
                var outcome = this.update(BundleHelper.getEntryResource(version, e));
                var location = outcome.getId().getValue();
                BundleHelper.addEntry(
                        returnBundle,
                        BundleHelper.newEntryWithResponse(
                                version, BundleHelper.newResponseWithLocation(version, location)));
            } else if (BundleHelper.isEntryRequestPost(version, e)) {
                var outcome = this.create(BundleHelper.getEntryResource(version, e));
                var location = outcome.getId().getValue();
                BundleHelper.addEntry(
                        returnBundle,
                        BundleHelper.newEntryWithResponse(
                                version, BundleHelper.newResponseWithLocation(version, location)));
            } else if (BundleHelper.isEntryRequestDelete(version, e)) {
                if (BundleHelper.getEntryRequestId(version, e).isPresent()) {
                    var resourceType = Canonicals.getResourceType(BundleHelper.getEntryRequestUrl(version, e));
                    var resourceClass =
                            this.context.getResourceDefinition(resourceType).getImplementingClass();
                    var res = this.delete(
                            resourceClass,
                            BundleHelper.getEntryRequestId(version, e).get().withResourceType(resourceType));
                    BundleHelper.addEntry(returnBundle, BundleHelper.newEntryWithResource(res.getResource()));
                } else {
                    throw new ResourceNotFoundException("Trying to delete an entry without id");
                }

            } else {
                throw new NotImplementedOperationException("Transaction stub only supports PUT, POST or DELETE");
            }
        });

        return returnBundle;
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters> R invoke(
            String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        checkNotNull(name, "name is required");
        checkNotNull(returnType, "returnType is required");
        checkNotNull(headers, "headers are required");
        try {
            var result = operationRegistry
                    .buildInvocationContext(this, name)
                    .parameters(parameters)
                    .execute();
            return returnType.cast(result);
        } catch (BaseServerResponseException e) {
            throw e;
        } catch (Throwable e) {
            throw new InternalErrorException(e);
        }
    }

    @Override
    public <P extends IBaseParameters> MethodOutcome invoke(String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedOperationException("Invoke is not currently supported");
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, T extends IBaseResource> R invoke(
            Class<T> resourceType, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        checkNotNull(resourceType, "resourceType is required");
        checkNotNull(name, "name is required");
        checkNotNull(returnType, "returnType is required");
        checkNotNull(headers, "headers are required");
        try {
            var result = operationRegistry
                    .buildInvocationContext(this, name)
                    .parameters(parameters)
                    .resourceType(resourceType)
                    .execute();
            return returnType.cast(result);
        } catch (BaseServerResponseException e) {
            throw e;
        } catch (Throwable e) {
            throw new InternalErrorException(e);
        }
    }

    @Override
    public <P extends IBaseParameters, T extends IBaseResource> MethodOutcome invoke(
            Class<T> resourceType, String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedOperationException("Invoke is not currently supported");
    }

    @Override
    public <R extends IBaseResource, P extends IBaseParameters, I extends IIdType> R invoke(
            I id, String name, P parameters, Class<R> returnType, Map<String, String> headers) {
        checkNotNull(id, "id is required");
        checkArgument(id.hasResourceType(), "resourceType is required for id scoped operations");
        checkNotNull(name, "name is required");
        checkNotNull(returnType, "returnType is required");
        checkNotNull(headers, "headers are required");
        try {
            var result = operationRegistry
                    .buildInvocationContext(this, name)
                    .parameters(parameters)
                    .id(id)
                    .resourceType(this.context
                            .getResourceDefinition(id.getResourceType())
                            .getImplementingClass())
                    .execute();
            return returnType.cast(result);
        } catch (BaseServerResponseException e) {
            throw e;
        } catch (Throwable e) {
            throw new InternalErrorException(e);
        }
    }

    @Override
    public <P extends IBaseParameters, I extends IIdType> MethodOutcome invoke(
            I id, String name, P parameters, Map<String, String> headers) {
        throw new NotImplementedOperationException("Invoke is not currently supported");
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters> B history(
            P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedOperationException("The history interaction is not currently supported");
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, T extends IBaseResource> B history(
            Class<T> resourceType, P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedOperationException("The history interaction is not currently supported");
    }

    @Override
    public <B extends IBaseBundle, P extends IBaseParameters, I extends IIdType> B history(
            I id, P parameters, Class<B> returnType, Map<String, String> headers) {
        throw new NotImplementedOperationException("The history interaction is not currently supported");
    }

    @Override
    @Nonnull
    public FhirContext fhirContext() {
        return this.context;
    }

    /**
     * Register an operation with the repository. This must be a class with at least one method annotated with
     * the @Operation annotation. The factory function is used to create an instance of the operation class on
     * a per-request basis. All methods on the operation class marked with the @Operation annotation will be
     * available for invocation.
     * @param <T> the type of the operation class
     * @param clazz the operation class
     * @param factory a factory function that will create an instance of the operation class
     */
    public <T> void registerOperation(Class<T> clazz, Function<IRepository, T> factory) {
        requireNonNull(clazz, "clazz can not be null");
        requireNonNull(factory, "factory can not be null");
        operationRegistry.register(clazz, factory);
    }
}

package org.opencds.cqf.fhir.cr.dev.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.util.BundleUtil;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.dev.server.search.SearchParameterTranslator;

/**
 * Bridges HAPI {@code RestfulServer}'s typed CRUD interactions to an {@code IRepository}. One
 * instance is registered per FHIR resource type; HAPI's annotation-driven dispatch routes
 * {@code GET/POST/PUT/DELETE/SEARCH Resource[/id]} to the methods below, which delegate to the
 * {@code IRepository} returned by {@link IRepositoryFactory#create(RequestDetails)}.
 *
 * <p>Until the canonical "gateway IRepository" lands (see ARCHITECTURE.md), this is the shim
 * that lets a server backed solely by an {@code IRepository} accept full CRUD + search over REST.
 *
 * <p>Conditional CRUD: HAPI binds {@code If-None-Exist} (conditional create) and {@code If-Match}
 * (conditional update/delete) to {@code @ConditionalUrlParam}; these flow into the request headers
 * map passed to {@code IRepository}, where supporting backends can act on them.
 */
@SuppressWarnings("UnstableApiUsage")
public class RepositoryResourceProvider<T extends IBaseResource> implements IResourceProvider {

    private final Class<T> resourceType;
    private final IRepositoryFactory repositoryFactory;
    private final FhirContext fhirContext;

    public RepositoryResourceProvider(
            Class<T> resourceType, IRepositoryFactory repositoryFactory, FhirContext fhirContext) {
        this.resourceType = resourceType;
        this.repositoryFactory = repositoryFactory;
        this.fhirContext = fhirContext;
    }

    @Override
    public Class<T> getResourceType() {
        return resourceType;
    }

    @Read
    public T read(@IdParam IIdType id, RequestDetails requestDetails) {
        return repositoryFactory.create(requestDetails).read(resourceType, id, headersOf(requestDetails));
    }

    @Create
    public MethodOutcome create(
            @ResourceParam T resource, @ConditionalUrlParam String conditionalUrl, RequestDetails requestDetails) {
        var headers = headersOf(requestDetails);
        if (conditionalUrl != null && !conditionalUrl.isEmpty()) {
            // Conditional-create per the FHIR spec is signalled via the If-None-Exist header,
            // which HAPI also surfaces as a ConditionalUrlParam. Forward both forms.
            headers.putIfAbsent(Constants.HEADER_IF_NONE_EXIST, conditionalUrl);
        }
        return repositoryFactory.create(requestDetails).create(resource, headers);
    }

    @Update
    public MethodOutcome update(
            @ResourceParam T resource,
            @IdParam IIdType id,
            @ConditionalUrlParam String conditionalUrl,
            RequestDetails requestDetails) {
        if (conditionalUrl != null && !conditionalUrl.isEmpty()) {
            // Conditional update by URL params (e.g. PUT /Patient?identifier=foo) is not yet
            // routed to IRepository — fail loudly rather than silently doing the wrong thing.
            throw new MethodNotAllowedException("Conditional update by URL is not supported by this server; "
                    + "use PUT /[type]/[id] with If-Match for optimistic concurrency.");
        }
        if (id != null && id.hasIdPart()) {
            // Make the URL id authoritative; align body id so the repository sees one id.
            resource.setId(id);
        }
        return repositoryFactory.create(requestDetails).update(resource, headersOf(requestDetails));
    }

    @Delete
    public MethodOutcome delete(
            @IdParam IIdType id, @ConditionalUrlParam String conditionalUrl, RequestDetails requestDetails) {
        if (conditionalUrl != null && !conditionalUrl.isEmpty()) {
            throw new MethodNotAllowedException(
                    "Conditional delete by URL is not supported by this server; " + "use DELETE /[type]/[id].");
        }
        return repositoryFactory.create(requestDetails).delete(resourceType, id, headersOf(requestDetails));
    }

    /**
     * Untyped search: convert raw URL params to typed {@code IQueryParameterType}s, hand to
     * {@link ca.uhn.fhir.repository.IRepository#search}, return the bundle as a bundle provider so
     * HAPI handles paging/serialization.
     */
    @Search(allowUnknownParams = true)
    public IBundleProvider search(RequestDetails requestDetails) {
        var rawParams = requestDetails.getParameters();
        var typedParams =
                SearchParameterTranslator.translate(fhirContext, fhirContext.getResourceType(resourceType), rawParams);

        @SuppressWarnings("unchecked")
        Class<IBaseBundle> bundleClass =
                (Class<IBaseBundle>) fhirContext.getResourceDefinition("Bundle").getImplementingClass();

        IBaseBundle bundle = repositoryFactory
                .create(requestDetails)
                .search(bundleClass, resourceType, typedParams, headersOf(requestDetails));

        List<IBaseResource> resources = BundleUtil.toListOfResources(fhirContext, bundle);
        return new SimpleBundleProvider(resources);
    }

    /**
     * Snapshot the FHIR-relevant request headers HAPI tracks. {@code RequestDetails} doesn't
     * expose an "all headers" view, so we forward only the headers {@code IRepository}
     * implementations actually consult (concurrency control, conditional create, content
     * negotiation hints).
     */
    static Map<String, String> headersOf(RequestDetails requestDetails) {
        if (requestDetails == null) return Map.of();
        var headers = new java.util.HashMap<String, String>();
        for (String name : FORWARDED_HEADERS) {
            String value = requestDetails.getHeader(name);
            if (value != null && !value.isEmpty()) {
                headers.put(name, value);
            }
        }
        return headers;
    }

    private static final List<String> FORWARDED_HEADERS = List.of(
            Constants.HEADER_IF_MATCH,
            Constants.HEADER_IF_NONE_MATCH,
            Constants.HEADER_IF_NONE_EXIST,
            Constants.HEADER_IF_MODIFIED_SINCE,
            Constants.HEADER_PREFER);
}

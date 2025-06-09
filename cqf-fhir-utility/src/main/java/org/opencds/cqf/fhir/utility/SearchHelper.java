package org.opencds.cqf.fhir.utility;

import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResourceFirstRep;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.search.Searches;

public class SearchHelper {

    private SearchHelper() {}

    @SuppressWarnings("unchecked")
    protected static Class<IBaseBundle> getBundleClass(IRepository repository) {
        return (Class<IBaseBundle>)
                repository.fhirContext().getResourceDefinition("Bundle").getImplementingClass();
    }

    /**
     * Gets a resource class
     *
     * @param repository the repository to search
     * @param resourceType String of the resource typeget
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Class<IBaseResource> getResourceClass(IRepository repository, String resourceType) {
        return (Class<IBaseResource>)
                repository.fhirContext().getResourceDefinition(resourceType).getImplementingClass();
    }

    /**
     * Reads a resource from the repository
     *
     * @param repository the repository to search
     * @param id IIdType of the resource
     * @return
     */
    public static IBaseResource readRepository(IRepository repository, IIdType id) {
        return repository.read(getResourceClass(repository, id.getResourceType()), id);
    }

    /**
     * Searches the given Repository and returns the first entry found
     *
     * @param <CanonicalType>
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return
     */
    public static <CanonicalType extends IPrimitiveType<String>> IBaseResource searchRepositoryByCanonical(
            IRepository repository, CanonicalType canonical) {

        var resourceType = getResourceType(repository, canonical);
        return searchRepositoryByCanonical(repository, canonical, resourceType);
    }

    /**
     * Gets the resource type for the given canonical, based on the convention that canonical
     * URLs are of the form [base]/[resourceType]/[tail]
     *
     * If the URL does not conform to the convention, the cqf-resourceType extension is used
     * to determine the type of the resource, if present.
     *
     * If no extension is present, the type of the canonical is assumed to be CodeSystem, on
     * the grounds that most (if not all) non-conventional URLs are for CodeSystem uris.
     *
     * @param <CanonicalType>
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return
     */
    public static <CanonicalType extends IPrimitiveType<String>> Class<? extends IBaseResource> getResourceType(
            IRepository repository, CanonicalType canonical) {
        Class<? extends IBaseResource> resourceType = null;
        try {
            var resourceTypeString = Canonicals.getResourceType(canonical);
            if (StringUtils.isEmpty(resourceTypeString)) {
                throw new DataFormatException();
            }
            resourceType = repository
                    .fhirContext()
                    .getResourceDefinition(resourceTypeString)
                    .getImplementingClass();
        } catch (DataFormatException e) {
            // Use the "cqf-resourceType" extension to figure this out, if it's present
            var cqfResourceTypeExt = getResourceTypeStringFromCqfResourceTypeExtension(canonical);
            if (cqfResourceTypeExt.isPresent()) {
                try {
                    resourceType = repository
                            .fhirContext()
                            .getResourceDefinition(cqfResourceTypeExt.get())
                            .getImplementingClass();
                } catch (DataFormatException | NullPointerException e2) {
                    throw new UnprocessableEntityException(
                            "cqf-resourceType extension contains invalid resource type: " + cqfResourceTypeExt.get());
                }
            } else {
                // NOTE: This is based on the assumption that only CodeSystems don't follow the canonical pattern...
                resourceType = repository
                        .fhirContext()
                        .getResourceDefinition("CodeSystem")
                        .getImplementingClass();
            }
        }
        return resourceType;
    }

    /**
     * Gets the resource type for the given canonical, based on the convention that canonical
     * URLs are of the form [base]/[resourceType]/[tail]
     *
     * If the URL does not conform to the convention, the cqf-resourceType extension is used
     * to determine the type of the resource, if present.
     *
     * If no extension is present, the type of the canonical is assumed to be CodeSystem, on
     * the grounds that most (if not all) non-conventional URLs are for CodeSystem uris.
     *
     * @param repository the repository to search
     * @param dependencyInfo the canonical url to search for
     * @return
     */
    public static Class<? extends IBaseResource> getResourceType(
            IRepository repository, IDependencyInfo dependencyInfo) {
        Class<? extends IBaseResource> resourceType = null;
        try {
            var resourceTypeString = Canonicals.getResourceType(dependencyInfo.getReference());
            if (StringUtils.isEmpty(resourceTypeString)) {
                throw new DataFormatException();
            }
            resourceType = repository
                    .fhirContext()
                    .getResourceDefinition(resourceTypeString)
                    .getImplementingClass();
        } catch (DataFormatException e) {
            // Use the "cqf-resourceType" extension to figure this out, if it's present
            var cqfResourceTypeExt = getResourceTypeStringFromCqfResourceTypeExtension(dependencyInfo.getExtension());
            if (cqfResourceTypeExt.isPresent()) {
                try {
                    resourceType = repository
                            .fhirContext()
                            .getResourceDefinition(cqfResourceTypeExt.get())
                            .getImplementingClass();
                } catch (DataFormatException | NullPointerException e2) {
                    throw new UnprocessableEntityException(
                            "cqf-resourceType extension contains invalid resource type: " + cqfResourceTypeExt.get());
                }
            } else {
                // NOTE: This is based on the assumption that only CodeSystems don't follow the canonical pattern...
                resourceType = repository
                        .fhirContext()
                        .getResourceDefinition("CodeSystem")
                        .getImplementingClass();
            }
        }
        return resourceType;
    }

    private static <CanonicalType extends IPrimitiveType<String>>
            Optional<String> getResourceTypeStringFromCqfResourceTypeExtension(CanonicalType canonical) {
        return getResourceTypeStringFromCqfResourceTypeExtension(getExtensions(canonical));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<String> getResourceTypeStringFromCqfResourceTypeExtension(
            List<? extends IBaseExtension> extensions) {
        return extensions.stream()
                .filter(ext -> ext.getUrl().contains("cqf-resourceType"))
                .findAny()
                .map(ext -> ((IPrimitiveType<String>) ext.getValue()).getValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <CanonicalType extends IPrimitiveType<String>> List<IBaseExtension> getExtensions(
            CanonicalType canonical) {
        if (canonical instanceof org.hl7.fhir.dstu3.model.PrimitiveType) {
            return ((org.hl7.fhir.dstu3.model.PrimitiveType<String>) canonical)
                    .getExtension().stream().map(ext -> (IBaseExtension) ext).collect(Collectors.toList());
        } else if (canonical instanceof org.hl7.fhir.r4.model.PrimitiveType) {
            return ((org.hl7.fhir.r4.model.PrimitiveType<String>) canonical)
                    .getExtension().stream().map(ext -> (IBaseExtension) ext).collect(Collectors.toList());
        } else if (canonical instanceof org.hl7.fhir.r5.model.PrimitiveType) {
            return ((org.hl7.fhir.r5.model.PrimitiveType<String>) canonical)
                    .getExtension().stream().map(ext -> (IBaseExtension) ext).collect(Collectors.toList());
        } else {
            throw new UnprocessableEntityException("Unsupported FHIR version for canonical: " + canonical.getValue());
        }
    }

    /**
     * Gets the resource type for the given canonical, based on the convention that canonical
     * URLs are of the form [base]/[resourceType]/[tail]
     *
     * If the URL does not conform to the convention, the type of the canonical is assumed to be CodeSystem, on
     * the grounds that most (if not all) non-conventional URLs are for CodeSystem uris.
     *
     * @param repository
     * @param canonical
     * @return
     */
    private static Class<? extends IBaseResource> getResourceType(IRepository repository, String canonical) {
        Class<? extends IBaseResource> resourceType = null;
        try {
            resourceType = repository
                    .fhirContext()
                    .getResourceDefinition(Canonicals.getResourceType(canonical))
                    .getImplementingClass();
        } catch (RuntimeException e) {
            // Can't use the "cqf-resourceType" extension to figure this out because we just get a canonical string
            // NOTE: This is based on the assumption that only CodeSystems don't follow the canonical pattern...
            resourceType =
                    repository.fhirContext().getResourceDefinition("CodeSystem").getImplementingClass();
        }

        return resourceType;
    }

    /**
     * Searches the given Repository and returns the first entry found
     *
     * @param <CanonicalType> an IPrimitiveType<String> type
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @return
     */
    public static <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource>
            IBaseResource searchRepositoryByCanonical(
                    IRepository repository, CanonicalType canonical, Class<R> resourceType) {
        var url = Canonicals.getUrl(canonical);
        var version = Canonicals.getVersion(canonical);
        var searchParams = version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
        var searchResult = repository.search(getBundleClass(repository), resourceType, searchParams);
        var result = getEntryResourceFirstRep(searchResult);
        if (result == null) {
            throw new FHIRException("No resource of type %s found for url: %s|%s"
                    .formatted(resourceType.getSimpleName(), url, version));
        }

        return result;
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <CanonicalType>
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return
     */
    public static <CanonicalType extends IPrimitiveType<String>> IBaseBundle searchRepositoryByCanonicalWithPaging(
            IRepository repository, CanonicalType canonical) {
        var resourceType = getResourceType(repository, canonical);

        return searchRepositoryByCanonicalWithPaging(repository, canonical, resourceType);
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <CanonicalType>
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return
     */
    public static <CanonicalType extends IPrimitiveType<String>> IBaseBundle searchRepositoryByCanonicalWithPaging(
            IRepository repository, String canonical) {
        var resourceType = getResourceType(repository, canonical);

        return searchRepositoryByCanonicalWithPaging(repository, canonical, resourceType);
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param additionalSearchParams search parameters to pass on to the repository
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return
     */
    public static IBaseBundle searchRepositoryByCanonicalWithPagingWithParams(
            IRepository repository, String canonical, Map<String, List<IQueryParameterType>> additionalSearchParams) {
        var resourceType = getResourceType(repository, canonical);
        return searchRepositoryByCanonicalWithPagingWithParams(
                repository, canonical, resourceType, additionalSearchParams);
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <CanonicalType> an IPrimitiveType<String> type
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @return
     */
    public static <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource>
            IBaseBundle searchRepositoryByCanonicalWithPaging(
                    IRepository repository, CanonicalType canonical, Class<R> resourceType) {
        return searchRepositoryByCanonicalWithPagingWithParams(repository, canonical, resourceType, null);
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <CanonicalType> an IPrimitiveType<String> type
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @param additionalSearchParams extra search parameters to search with
     * @return
     */
    public static <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource>
            IBaseBundle searchRepositoryByCanonicalWithPagingWithParams(
                    IRepository repository,
                    CanonicalType canonical,
                    Class<R> resourceType,
                    Map<String, List<IQueryParameterType>> additionalSearchParams) {
        var url = Canonicals.getUrl(canonical);
        var version = Canonicals.getVersion(canonical);
        var searchParams = version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
        if (additionalSearchParams != null) {
            searchParams.putAll(additionalSearchParams);
        }
        var searchResult = searchRepositoryWithPaging(repository, resourceType, searchParams, Collections.emptyMap());

        return searchResult;
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @return
     */
    public static <R extends IBaseResource> IBaseBundle searchRepositoryByCanonicalWithPaging(
            IRepository repository, String canonical, Class<R> resourceType) {
        return searchRepositoryByCanonicalWithPagingWithParams(repository, canonical, resourceType, null);
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @param additionalSearchParams extra search parameters to search with
     * @return
     */
    public static <R extends IBaseResource> IBaseBundle searchRepositoryByCanonicalWithPagingWithParams(
            IRepository repository,
            String canonical,
            Class<R> resourceType,
            Map<String, List<IQueryParameterType>> additionalSearchParams) {
        var url = Canonicals.getUrl(canonical);
        var version = Canonicals.getVersion(canonical);
        var searchParams = version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
        if (additionalSearchParams != null) {
            searchParams.putAll(additionalSearchParams);
        }
        var searchResult = searchRepositoryWithPaging(repository, resourceType, searchParams, Collections.emptyMap());

        return searchResult;
    }

    /**
     * Searches the given Repository and handles paging to return all resources found in the search
     *
     * @param <T> an IBaseResource type
     * @param <R> an IBaseBundle type
     * @param repository the repository to search
     * @param resourceType the class of the resource being searched for
     * @param searchParameters the search parameters
     * @param headers the search headers
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends IBaseResource, R extends IBaseBundle> R searchRepositoryWithPaging(
            IRepository repository,
            Class<T> resourceType,
            Map<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        var bundleClass = getBundleClass(repository);
        var result = repository.search(bundleClass, resourceType, searchParameters, headers);
        handlePaging(repository, result);

        return (R) result;
    }

    private static void handlePaging(IRepository repository, IBaseBundle bundle) {
        var fhirVersion = repository.fhirContext().getVersion().getVersion();
        switch (fhirVersion) {
            case DSTU3:
                var dstu3Bundle = (org.hl7.fhir.dstu3.model.Bundle) bundle;
                var dstu3Next = dstu3Bundle.getLink(IBaseBundle.LINK_NEXT);
                if (dstu3Next != null) {
                    getNextPageDstu3(repository, dstu3Bundle, dstu3Next.getUrl());
                }
                break;
            case R4:
                var r4Bundle = (org.hl7.fhir.r4.model.Bundle) bundle;
                var r4Next = r4Bundle.getLink(IBaseBundle.LINK_NEXT);
                if (r4Next != null) {
                    getNextPageR4(repository, r4Bundle, r4Next.getUrl());
                }
                break;
            case R5:
                var r5Bundle = (org.hl7.fhir.r5.model.Bundle) bundle;
                var r5Next = r5Bundle.getLink(IBaseBundle.LINK_NEXT);
                if (r5Next != null) {
                    getNextPageR5(repository, r5Bundle, r5Next.getUrl());
                }
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported version of FHIR: %s".formatted(fhirVersion.getFhirVersionString()));
        }
    }

    private static void getNextPageDstu3(
            IRepository repository, org.hl7.fhir.dstu3.model.Bundle bundle, String nextUrl) {
        var nextBundle =
                (org.hl7.fhir.dstu3.model.Bundle) repository.link(org.hl7.fhir.dstu3.model.Bundle.class, nextUrl);
        nextBundle.getEntry().forEach(bundle::addEntry);
        var next = nextBundle.getLink(IBaseBundle.LINK_NEXT);
        if (next != null) {
            getNextPageDstu3(repository, bundle, next.getUrl());
        }
    }

    private static void getNextPageR4(IRepository repository, org.hl7.fhir.r4.model.Bundle bundle, String nextUrl) {
        var nextBundle = repository.link(org.hl7.fhir.r4.model.Bundle.class, nextUrl);
        nextBundle.getEntry().forEach(bundle::addEntry);
        var next = nextBundle.getLink(IBaseBundle.LINK_NEXT);
        if (next != null) {
            getNextPageR4(repository, bundle, next.getUrl());
        }
    }

    private static void getNextPageR5(IRepository repository, org.hl7.fhir.r5.model.Bundle bundle, String nextUrl) {
        var nextBundle = repository.link(org.hl7.fhir.r5.model.Bundle.class, nextUrl);
        nextBundle.getEntry().forEach(bundle::addEntry);
        var next = nextBundle.getLink(IBaseBundle.LINK_NEXT);
        if (next != null) {
            getNextPageR5(repository, bundle, next.getUrl());
        }
    }
}

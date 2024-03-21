package org.opencds.cqf.fhir.utility;

import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResourceFirstRep;

import ca.uhn.fhir.model.api.IQueryParameterType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.search.Searches;

public class SearchHelper {

    private SearchHelper() {}

    @SuppressWarnings("unchecked")
    protected static Class<IBaseBundle> getBundleClass(Repository repository) {
        return (Class<IBaseBundle>)
                repository.fhirContext().getResourceDefinition("Bundle").getImplementingClass();
    }

    @SuppressWarnings("unchecked")
    protected static Class<IBaseResource> getResourceClass(Repository repository, String resourceType) {
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
    public static IBaseResource readRepository(Repository repository, IIdType id) {
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
            Repository repository, CanonicalType canonical) {
        var resourceType = repository
                .fhirContext()
                .getResourceDefinition(Canonicals.getResourceType(canonical))
                .getImplementingClass();

        return searchRepositoryByCanonical(repository, canonical, resourceType);
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
                    Repository repository, CanonicalType canonical, Class<R> resourceType) {
        var url = Canonicals.getUrl(canonical);
        var version = Canonicals.getVersion(canonical);
        var searchParams = version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
        var searchResult = repository.search(getBundleClass(repository), resourceType, searchParams);
        var result = getEntryResourceFirstRep(searchResult);
        if (result == null) {
            throw new FHIRException(String.format(
                    "No resource of type %s found for url: %s|%s", resourceType.getSimpleName(), url, version));
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
            Repository repository, CanonicalType canonical) {
        var resourceType = repository
                .fhirContext()
                .getResourceDefinition(Canonicals.getResourceType(canonical))
                .getImplementingClass();

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
            Repository repository, String canonical) {
        var resourceType = repository
                .fhirContext()
                .getResourceDefinition(Canonicals.getResourceType(canonical))
                .getImplementingClass();

        return searchRepositoryByCanonicalWithPaging(repository, canonical, resourceType);
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
                    Repository repository, CanonicalType canonical, Class<R> resourceType) {
        var url = Canonicals.getUrl(canonical);
        var version = Canonicals.getVersion(canonical);
        var searchParams = version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
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
            Repository repository, String canonical, Class<R> resourceType) {
        var url = Canonicals.getUrl(canonical);
        var version = Canonicals.getVersion(canonical);
        var searchParams = version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
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
            Repository repository,
            Class<T> resourceType,
            Map<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        var bundleClass = getBundleClass(repository);
        var result = repository.search(bundleClass, resourceType, searchParameters, headers);
        handlePaging(repository, result);

        return (R) result;
    }

    private static void handlePaging(Repository repository, IBaseBundle bundle) {
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
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    private static void getNextPageDstu3(
            Repository repository, org.hl7.fhir.dstu3.model.Bundle bundle, String nextUrl) {
        var nextBundle =
                (org.hl7.fhir.dstu3.model.Bundle) repository.link(org.hl7.fhir.dstu3.model.Bundle.class, nextUrl);
        nextBundle.getEntry().forEach(bundle::addEntry);
        var next = nextBundle.getLink(IBaseBundle.LINK_NEXT);
        if (next != null) {
            getNextPageDstu3(repository, bundle, next.getUrl());
        }
    }

    private static void getNextPageR4(Repository repository, org.hl7.fhir.r4.model.Bundle bundle, String nextUrl) {
        var nextBundle = repository.link(org.hl7.fhir.r4.model.Bundle.class, nextUrl);
        nextBundle.getEntry().forEach(bundle::addEntry);
        var next = nextBundle.getLink(IBaseBundle.LINK_NEXT);
        if (next != null) {
            getNextPageR4(repository, bundle, next.getUrl());
        }
    }

    private static void getNextPageR5(Repository repository, org.hl7.fhir.r5.model.Bundle bundle, String nextUrl) {
        var nextBundle = repository.link(org.hl7.fhir.r5.model.Bundle.class, nextUrl);
        nextBundle.getEntry().forEach(bundle::addEntry);
        var next = nextBundle.getLink(IBaseBundle.LINK_NEXT);
        if (next != null) {
            getNextPageR5(repository, bundle, next.getUrl());
        }
    }
}

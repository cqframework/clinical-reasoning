package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IQueryParameterType;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.search.Searches;

public class SearchHelper {

    private SearchHelper() {}

    @SuppressWarnings("unchecked")
    protected static Class<IBaseBundle> getBundleType(Repository repository) {
        return (Class<IBaseBundle>)
                repository.fhirContext().getResourceDefinition("Bundle").getImplementingClass();
    }

    public static <CanonicalType extends IPrimitiveType<String>> IBaseResource searchRepositoryByCanonical(
            Repository repository, CanonicalType canonical) {
        var resourceType = repository
                .fhirContext()
                .getResourceDefinition(Canonicals.getResourceType(canonical))
                .getImplementingClass();

        return searchRepositoryByCanonical(repository, canonical, resourceType);
    }

    public static <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource>
            IBaseResource searchRepositoryByCanonical(
                    Repository repository, CanonicalType canonical, Class<R> resourceType) {
        var url = Canonicals.getUrl(canonical);
        var version = Canonicals.getVersion(canonical);

        var searchParams = version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
        var searchResult = repository.search(getBundleType(repository), resourceType, searchParams);
        var result = getEntryFirstRep(
                searchResult, repository.fhirContext().getVersion().getVersion());
        if (result == null) {
            throw new FHIRException(String.format(
                    "No resource of type %s found for url: %s|%s", resourceType.getSimpleName(), url, version));
        }

        return result;
    }

    protected static IBaseResource getEntryFirstRep(IBaseBundle bundle, FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                var dstu3Entry = ((org.hl7.fhir.dstu3.model.Bundle) bundle).getEntryFirstRep();
                return dstu3Entry != null && dstu3Entry.hasResource() ? dstu3Entry.getResource() : null;
            case R4:
                var r4Entry = ((org.hl7.fhir.r4.model.Bundle) bundle).getEntryFirstRep();
                return r4Entry != null && r4Entry.hasResource() ? r4Entry.getResource() : null;
            case R5:
                var r5Entry = ((org.hl7.fhir.r5.model.Bundle) bundle).getEntryFirstRep();
                return r5Entry != null && r5Entry.hasResource() ? r5Entry.getResource() : null;

            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseResource, R extends IBaseBundle> R searchRepositoryWithPaging(
            Repository repository,
            Class<T> resourceType,
            Map<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        var bundleClass = getBundleType(repository);
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

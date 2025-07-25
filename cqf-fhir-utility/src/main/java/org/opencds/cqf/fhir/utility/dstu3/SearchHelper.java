package org.opencds.cqf.fhir.utility.dstu3;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.search.Searches;

public class SearchHelper {

    private SearchHelper() {}

    public static <CanonicalType extends IPrimitiveType<String>> Resource searchRepositoryByCanonical(
            IRepository repository, CanonicalType canonical) {
        if (canonical == null) {
            return null;
        }
        var url = Canonicals.getUrl(canonical);
        var version = Canonicals.getVersion(canonical);
        var resourceType = repository
                .fhirContext()
                .getResourceDefinition(Canonicals.getResourceType(canonical))
                .getImplementingClass();

        var searchParams = version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
        var searchResult = repository.search(Bundle.class, resourceType, searchParams);
        if (!searchResult.hasEntry()) {
            throw new FHIRException("No resource of type %s found for url: %s|%s"
                    .formatted(resourceType.getSimpleName(), url, version));
        }

        return searchResult.getEntryFirstRep().getResource();
    }

    public static <T extends IBaseResource> Bundle searchRepositoryWithPaging(
            IRepository repository,
            Class<T> resourceType,
            Map<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {
        var result = repository.search(Bundle.class, resourceType, searchParameters, headers);
        var next = result.getLink(IBaseBundle.LINK_NEXT);
        if (next != null) {
            getNextPage(repository, result, next.getUrl());
        }

        return result;
    }

    private static void getNextPage(IRepository repository, Bundle bundle, String nextUrl) {
        var nextBundle = repository.link(Bundle.class, nextUrl);
        nextBundle.getEntry().forEach(bundle::addEntry);
        var next = nextBundle.getLink(IBaseBundle.LINK_NEXT);
        if (next != null) {
            getNextPage(repository, bundle, next.getUrl());
        }
    }
}

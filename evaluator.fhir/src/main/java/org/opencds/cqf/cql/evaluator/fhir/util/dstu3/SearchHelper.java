package org.opencds.cqf.cql.evaluator.fhir.util.dstu3;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Searches;

import ca.uhn.fhir.model.api.IQueryParameterType;

public class SearchHelper {

  private SearchHelper() {}

  public static <CanonicalType extends IPrimitiveType<String>> Resource searchRepositoryByCanonical(
      Repository theRepository, CanonicalType theCanonical) {
    var url = Canonicals.getUrl(theCanonical);
    var version = Canonicals.getVersion(theCanonical);
    var resourceType = theRepository.fhirContext()
        .getResourceDefinition(Canonicals.getResourceType(theCanonical)).getImplementingClass();

    var searchParams =
        version == null ? Searches.byUrl(url) : Searches.byUrlAndVersion(url, version);
    var searchResult =
        theRepository.search(Bundle.class, resourceType, searchParams);
    if (!searchResult.hasEntry()) {
      throw new FHIRException(String.format("No resource of type %s found for url: %s|%s",
          resourceType.getSimpleName(), url, version));
    }

    return searchResult.getEntryFirstRep().getResource();
  }

  public static <T extends IBaseResource> Bundle searchRepositoryWithPaging(Repository repository,
      Class<T> resourceType, Map<String, List<IQueryParameterType>> searchParameters,
      Map<String, String> headers) {
    var result = repository.search(Bundle.class, resourceType, searchParameters, headers);
    var next = result.getLink(IBaseBundle.LINK_NEXT);
    if (next != null) {
      getNextPage(repository, result, next.getUrl());
    }

    return result;
  }

  private static void getNextPage(Repository repository, Bundle bundle, String nextUrl) {
    var nextBundle = repository.link(Bundle.class, nextUrl);
    nextBundle.getEntry().forEach(bundle::addEntry);
    var next = nextBundle.getLink(IBaseBundle.LINK_NEXT);
    if (next != null) {
      getNextPage(repository, bundle, next.getUrl());
    }
  }
}

package org.opencds.cqf.cql.evaluator.fhir.util.r5;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Searches;

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
}

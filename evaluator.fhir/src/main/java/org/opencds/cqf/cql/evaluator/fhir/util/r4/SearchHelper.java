package org.opencds.cqf.cql.evaluator.fhir.util.r4;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
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

    var searchResult =
        theRepository.search(Bundle.class, resourceType, Searches.byUrlAndVersion(url, version));
    if (!searchResult.hasEntry()) {
      throw new FHIRException(String.format("No resource of type %s found for url: %s|%s",
          resourceType.getSimpleName(), url, version));
    }

    return searchResult.getEntryFirstRep().getResource();
  }
}

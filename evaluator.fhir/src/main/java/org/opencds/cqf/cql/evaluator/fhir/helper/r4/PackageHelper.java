package org.opencds.cqf.cql.evaluator.fhir.helper.r4;

import static org.opencds.cqf.cql.evaluator.fhir.util.r4.SearchHelper.searchRepositoryByCanonical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.util.Canonicals;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class consists exclusively of static methods that assist with packaging FHIR Resources.
 */
public class PackageHelper {

  private PackageHelper() {}

  protected static final Logger ourLogger = LoggerFactory.getLogger(PackageHelper.class);

  protected static final List<String> PACKABLE_RESOURCES =
      Arrays.asList(FHIRAllTypes.LIBRARY.toCode(), FHIRAllTypes.PLANDEFINITION.toCode(),
          FHIRAllTypes.ACTIVITYDEFINITION.toCode(), FHIRAllTypes.STRUCTUREDEFINITION.toCode(),
          FHIRAllTypes.CODESYSTEM.toCode(), FHIRAllTypes.VALUESET.toCode());

  protected static boolean hasRelatedArtifact(Resource theResource) {
    return (theResource.fhirType().equals(FHIRAllTypes.LIBRARY.toCode())
        && ((Library) theResource).hasRelatedArtifact())
        || (theResource.fhirType().equals(FHIRAllTypes.PLANDEFINITION.toCode())
            && ((PlanDefinition) theResource).hasRelatedArtifact())
        || (theResource.fhirType().equals(FHIRAllTypes.ACTIVITYDEFINITION.toCode())
            && ((ActivityDefinition) theResource).hasRelatedArtifact());
  }

  protected static List<RelatedArtifact> getRelatedArtifact(Resource theResource) {
    List<RelatedArtifact> relatedArtifact = new ArrayList<>();
    switch (theResource.getResourceType()) {
      case Library:
        return ((Library) theResource).getRelatedArtifact();

      case PlanDefinition:
        return ((PlanDefinition) theResource).getRelatedArtifact();

      case ActivityDefinition:
        return ((ActivityDefinition) theResource).getRelatedArtifact();

      default:
        break;
    }

    return relatedArtifact;
  }

  public static BundleEntryComponent createEntry(Resource theResource) {
    var url = theResource.getResourceType().toString() + "/" + theResource.getIdPart();
    return new BundleEntryComponent().setResource(theResource)
        .setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.PUT).setUrl(url));
  }

  public static void addRelatedArtifacts(Bundle theBundle, List<RelatedArtifact> theArtifacts,
      Repository theRepository) {
    for (var artifact : theArtifacts) {
      if (artifact.getType().equals(RelatedArtifactType.DEPENDSON)
          && artifact.hasResourceElement()) {
        try {
          var canonical = artifact.getResourceElement();
          if (PACKABLE_RESOURCES.contains(Canonicals.getResourceType(canonical))) {
            var resource =
                searchRepositoryByCanonical(theRepository, artifact.getResourceElement());
            if (resource != null
                && theBundle.getEntry().stream()
                    .noneMatch(
                        e -> e.getResource().getIdElement().equals(resource.getIdElement()))) {
              theBundle.addEntry(createEntry(resource));
              if (hasRelatedArtifact(resource)) {
                addRelatedArtifacts(theBundle, getRelatedArtifact(resource), theRepository);
              }
            }
          }
        } catch (Exception e) {
          ourLogger.error(e.getMessage(), e);
        }
      }
    }
  }


}

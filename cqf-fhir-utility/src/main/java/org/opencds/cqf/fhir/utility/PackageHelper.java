package org.opencds.cqf.fhir.utility;

import static org.opencds.cqf.fhir.utility.r4.SearchHelper.searchRepositoryByCanonical;

import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class consists exclusively of static methods that assist with packaging FHIR Resources.
 */
public class PackageHelper {

    private PackageHelper() {}

    protected static final Logger logger = LoggerFactory.getLogger(PackageHelper.class);

    protected static final List<String> PACKABLE_RESOURCES = Arrays.asList(
            FHIRAllTypes.LIBRARY.toCode(),
            FHIRAllTypes.PLANDEFINITION.toCode(),
            FHIRAllTypes.ACTIVITYDEFINITION.toCode(),
            FHIRAllTypes.STRUCTUREDEFINITION.toCode(),
            FHIRAllTypes.CODESYSTEM.toCode(),
            FHIRAllTypes.VALUESET.toCode());

    protected static boolean hasRelatedArtifact(Resource theResource) {
        switch (theResource.getResourceType()) {
            case Library:
                return ((Library) theResource).hasRelatedArtifact();
            case PlanDefinition:
                return ((PlanDefinition) theResource).hasRelatedArtifact();
            case ActivityDefinition:
                return ((ActivityDefinition) theResource).hasRelatedArtifact();
            default:
                return false;
        }
    }

    protected static List<RelatedArtifact> getRelatedArtifact(Resource theResource) {
        switch (theResource.getResourceType()) {
            case Library:
                return ((Library) theResource).getRelatedArtifact();
            case PlanDefinition:
                return ((PlanDefinition) theResource).getRelatedArtifact();
            case ActivityDefinition:
                return ((ActivityDefinition) theResource).getRelatedArtifact();
            default:
                return null;
        }
    }

    protected static boolean hasUrl(Resource theResource) {
        switch (theResource.getResourceType()) {
            case Library:
                return ((Library) theResource).hasUrl();
            case PlanDefinition:
                return ((PlanDefinition) theResource).hasUrl();
            case ActivityDefinition:
                return ((ActivityDefinition) theResource).hasUrl();
            case StructureDefinition:
                return ((StructureDefinition) theResource).hasUrl();
            case ValueSet:
                return ((ValueSet) theResource).hasUrl();
            case CodeSystem:
                return ((CodeSystem) theResource).hasUrl();
            case Questionnaire:
                return ((Questionnaire) theResource).hasUrl();
            default:
                return false;
        }
    }

    protected static String getUrl(Resource theResource) {
        switch (theResource.getResourceType()) {
            case Library:
                return ((Library) theResource).getUrl();
            case PlanDefinition:
                return ((PlanDefinition) theResource).getUrl();
            case ActivityDefinition:
                return ((ActivityDefinition) theResource).getUrl();
            case StructureDefinition:
                return ((StructureDefinition) theResource).getUrl();
            case ValueSet:
                return ((ValueSet) theResource).getUrl();
            case CodeSystem:
                return ((CodeSystem) theResource).getUrl();
            case Questionnaire:
                return ((Questionnaire) theResource).getUrl();
            default:
                return null;
        }
    }

    protected static boolean hasVersion(Resource theResource) {
        switch (theResource.getResourceType()) {
            case Library:
                return ((Library) theResource).hasVersion();
            case PlanDefinition:
                return ((PlanDefinition) theResource).hasVersion();
            case ActivityDefinition:
                return ((ActivityDefinition) theResource).hasVersion();
            case StructureDefinition:
                return ((StructureDefinition) theResource).hasVersion();
            case ValueSet:
                return ((ValueSet) theResource).hasVersion();
            case CodeSystem:
                return ((CodeSystem) theResource).hasVersion();
            case Questionnaire:
                return ((Questionnaire) theResource).hasVersion();
            default:
                return false;
        }
    }

    protected static String getVersion(Resource theResource) {
        switch (theResource.getResourceType()) {
            case Library:
                return ((Library) theResource).getVersion();
            case PlanDefinition:
                return ((PlanDefinition) theResource).getVersion();
            case ActivityDefinition:
                return ((ActivityDefinition) theResource).getVersion();
            case StructureDefinition:
                return ((StructureDefinition) theResource).getVersion();
            case ValueSet:
                return ((ValueSet) theResource).getVersion();
            case CodeSystem:
                return ((CodeSystem) theResource).getVersion();
            case Questionnaire:
                return ((Questionnaire) theResource).getVersion();
            default:
                return null;
        }
    }

    public static BundleEntryComponent createEntry(Resource theResource, boolean theIsPut) {
        var resourceType = theResource.getResourceType().toString();
        var entry = new BundleEntryComponent().setResource(theResource);
        var request = new BundleEntryRequestComponent();
        if (theIsPut) {
            request.setMethod(HTTPVerb.PUT).setUrl(resourceType + "/" + theResource.getIdPart());
        } else {
            request.setMethod(HTTPVerb.POST).setUrl(resourceType);
            if (hasUrl(theResource)) {
                var url = getUrl(theResource);
                if (hasVersion(theResource)) {
                    request.setIfNoneExist(String.format("url=%s&version=%s", url, getVersion(theResource)));
                } else {
                    request.setIfNoneExist(String.format("url=%s", url));
                }
            }
        }
        entry.setRequest(request);

        return entry;
    }

    public static void addRelatedArtifacts(
            Bundle theBundle, List<RelatedArtifact> theArtifacts, Repository theRepository, boolean theIsPut) {
        for (var artifact : theArtifacts) {
            if (artifact.getType().equals(RelatedArtifactType.DEPENDSON) && artifact.hasResourceElement()) {
                try {
                    var canonical = artifact.getResourceElement();
                    if (PACKABLE_RESOURCES.contains(Canonicals.getResourceType(canonical))) {
                        var resource = searchRepositoryByCanonical(theRepository, canonical);
                        if (resource != null
                                && theBundle.getEntry().stream()
                                        .noneMatch(e ->
                                                e.getResource().getIdElement().equals(resource.getIdElement()))) {
                            theBundle.addEntry(createEntry(resource, theIsPut));
                            if (hasRelatedArtifact(resource)) {
                                addRelatedArtifacts(theBundle, getRelatedArtifact(resource), theRepository, theIsPut);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}

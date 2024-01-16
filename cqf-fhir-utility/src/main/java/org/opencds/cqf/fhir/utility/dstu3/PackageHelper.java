package org.opencds.cqf.fhir.utility.dstu3;

import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
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

    protected static boolean hasRelatedArtifact(Resource resource) {
        switch (resource.getResourceType()) {
            case Library:
                return ((Library) resource).hasRelatedArtifact();
            case PlanDefinition:
                return ((PlanDefinition) resource).hasRelatedArtifact();
            case ActivityDefinition:
                return ((ActivityDefinition) resource).hasRelatedArtifact();
            default:
                return false;
        }
    }

    protected static List<RelatedArtifact> getRelatedArtifact(Resource resource) {
        switch (resource.getResourceType()) {
            case Library:
                return ((Library) resource).getRelatedArtifact();
            case PlanDefinition:
                return ((PlanDefinition) resource).getRelatedArtifact();
            case ActivityDefinition:
                return ((ActivityDefinition) resource).getRelatedArtifact();
            default:
                return null;
        }
    }

    protected static boolean hasUrl(Resource resource) {
        switch (resource.getResourceType()) {
            case Library:
                return ((Library) resource).hasUrl();
            case PlanDefinition:
                return ((PlanDefinition) resource).hasUrl();
            case ActivityDefinition:
                return ((ActivityDefinition) resource).hasUrl();
            case StructureDefinition:
                return ((StructureDefinition) resource).hasUrl();
            case ValueSet:
                return ((ValueSet) resource).hasUrl();
            case CodeSystem:
                return ((CodeSystem) resource).hasUrl();
            case Questionnaire:
                return ((Questionnaire) resource).hasUrl();
            default:
                return false;
        }
    }

    protected static String getUrl(Resource resource) {
        switch (resource.getResourceType()) {
            case Library:
                return ((Library) resource).getUrl();
            case PlanDefinition:
                return ((PlanDefinition) resource).getUrl();
            case ActivityDefinition:
                return ((ActivityDefinition) resource).getUrl();
            case StructureDefinition:
                return ((StructureDefinition) resource).getUrl();
            case ValueSet:
                return ((ValueSet) resource).getUrl();
            case CodeSystem:
                return ((CodeSystem) resource).getUrl();
            case Questionnaire:
                return ((Questionnaire) resource).getUrl();
            default:
                return null;
        }
    }

    protected static boolean hasVersion(Resource resource) {
        switch (resource.getResourceType()) {
            case Library:
                return ((Library) resource).hasVersion();
            case PlanDefinition:
                return ((PlanDefinition) resource).hasVersion();
            case ActivityDefinition:
                return ((ActivityDefinition) resource).hasVersion();
            case StructureDefinition:
                return ((StructureDefinition) resource).hasVersion();
            case ValueSet:
                return ((ValueSet) resource).hasVersion();
            case CodeSystem:
                return ((CodeSystem) resource).hasVersion();
            case Questionnaire:
                return ((Questionnaire) resource).hasVersion();
            default:
                return false;
        }
    }

    protected static String getVersion(Resource resource) {
        switch (resource.getResourceType()) {
            case Library:
                return ((Library) resource).getVersion();
            case PlanDefinition:
                return ((PlanDefinition) resource).getVersion();
            case ActivityDefinition:
                return ((ActivityDefinition) resource).getVersion();
            case StructureDefinition:
                return ((StructureDefinition) resource).getVersion();
            case ValueSet:
                return ((ValueSet) resource).getVersion();
            case CodeSystem:
                return ((CodeSystem) resource).getVersion();
            case Questionnaire:
                return ((Questionnaire) resource).getVersion();
            default:
                return null;
        }
    }

    public static BundleEntryComponent createEntry(Resource resource, boolean isPut) {
        var resourceType = resource.getResourceType().toString();
        var entry = new BundleEntryComponent().setResource(resource);
        var request = new BundleEntryRequestComponent();
        if (isPut) {
            request.setMethod(HTTPVerb.PUT).setUrl(resourceType + "/" + resource.getIdPart());
        } else {
            request.setMethod(HTTPVerb.POST).setUrl(resourceType);
            if (hasUrl(resource)) {
                var url = getUrl(resource);
                if (hasVersion(resource)) {
                    request.setIfNoneExist(String.format("url=%s&version=%s", url, getVersion(resource)));
                } else {
                    request.setIfNoneExist(String.format("url=%s", url));
                }
            }
        }
        entry.setRequest(request);

        return entry;
    }

    public static void addRelatedArtifacts(
            Bundle bundle, List<RelatedArtifact> theArtifacts, Repository repository, boolean isPut) {
        for (var artifact : theArtifacts) {
            if (artifact.getType().equals(RelatedArtifactType.DEPENDSON) && artifact.hasResource()) {
                try {
                    var canonical = artifact.getResource().getReference();
                    if (PACKABLE_RESOURCES.contains(Canonicals.getResourceType(canonical))) {
                        var resource = SearchHelper.searchRepositoryByCanonical(repository, new StringType(canonical));
                        if (resource != null
                                && bundle.getEntry().stream()
                                        .noneMatch(e ->
                                                e.getResource().getIdElement().equals(resource.getIdElement()))) {
                            bundle.addEntry(createEntry(resource, isPut));
                            if (hasRelatedArtifact(resource)) {
                                addRelatedArtifacts(bundle, getRelatedArtifact(resource), repository, isPut);
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

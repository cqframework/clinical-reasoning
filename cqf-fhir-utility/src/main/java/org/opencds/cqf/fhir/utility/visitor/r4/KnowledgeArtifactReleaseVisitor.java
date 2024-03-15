package org.opencds.cqf.fhir.utility.visitor.r4;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment;
import org.opencds.cqf.fhir.utility.r4.CRMIReleaseExperimentalBehavior.CRMIReleaseExperimentalBehaviorCodes;
import org.opencds.cqf.fhir.utility.r4.CRMIReleaseVersionBehavior.CRMIReleaseVersionBehaviorCodes;
import org.slf4j.Logger;

public class KnowledgeArtifactReleaseVisitor {

    public static void checkNonExperimental(
            MetadataResource resource,
            CRMIReleaseExperimentalBehaviorCodes experimentalBehavior,
            Repository repository,
            Logger log)
            throws UnprocessableEntityException {
        if (CRMIReleaseExperimentalBehaviorCodes.NULL != experimentalBehavior
                && CRMIReleaseExperimentalBehaviorCodes.NONE != experimentalBehavior) {
            String nonExperimentalError = String.format(
                    "Root artifact is not Experimental, but references an Experimental resource with URL '%s'.",
                    resource.getUrl());
            if (CRMIReleaseExperimentalBehaviorCodes.WARN == experimentalBehavior && resource.getExperimental()) {
                log.warn(nonExperimentalError);
            } else if (CRMIReleaseExperimentalBehaviorCodes.ERROR == experimentalBehavior
                    && resource.getExperimental()) {
                throw new UnprocessableEntityException(nonExperimentalError);
            }
            // for ValueSets need to check recursively if any chldren are experimental since we don't own these
            if (resource.getResourceType().equals(ResourceType.ValueSet)) {
                var valueSet = (ValueSet) resource;
                List<IPrimitiveType<String>> valueSets = valueSet.getCompose().getInclude().stream()
                        .flatMap(include -> include.getValueSet().stream())
                        .collect(Collectors.toList());
                for (var value : valueSets) {
                    KnowledgeArtifactAdapter.findLatestVersion(
                                    SearchHelper.searchRepositoryByCanonicalWithPaging(repository, value))
                            .ifPresent(childVs -> checkNonExperimental(
                                    (MetadataResource) childVs, experimentalBehavior, repository, log));
                }
            }
        }
    }

    public static void propagageEffectivePeriod(Period rootEffectivePeriod, KnowledgeArtifactAdapter artifactAdapter) {
        Period effectivePeriod = (Period) artifactAdapter.getEffectivePeriod();
        // if the root artifact period is NOT null AND HAS a start or an end date
        if ((rootEffectivePeriod != null && (rootEffectivePeriod.hasStart() || rootEffectivePeriod.hasEnd()))
                // and the current artifact period IS null OR does NOT HAVE a start or an end date
                && (effectivePeriod == null || !(effectivePeriod.hasStart() || effectivePeriod.hasEnd()))) {
            artifactAdapter.setEffectivePeriod(rootEffectivePeriod);
        }
    }

    public static void updateReleaseLabel(MetadataResource artifact, String releaseLabel)
            throws IllegalArgumentException {
        if (releaseLabel != null) {
            var releaseLabelExtension = artifact.getExtensionByUrl(KnowledgeArtifactAdapter.releaseLabelUrl);
            if (releaseLabelExtension == null) {
                // create the Extension and add it to the artifact if it doesn't exist
                releaseLabelExtension = new Extension(KnowledgeArtifactAdapter.releaseLabelUrl);
                artifact.addExtension(releaseLabelExtension);
            }
            releaseLabelExtension.setValue(new StringType(releaseLabel));
        }
    }

    public static Bundle searchArtifactAssessmentForArtifact(IIdType reference, Repository repository) {
        Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
        List<IQueryParameterType> urlList = new ArrayList<>();
        urlList.add(new ReferenceParam(reference));
        searchParams.put("artifact", urlList);
        Bundle searchResultsBundle = repository.search(Bundle.class, Basic.class, searchParams);
        return searchResultsBundle;
    }

    public static Optional<String> getReleaseVersion(
            String version, Optional<String> versionBehavior, String existingVersion) {
        Optional<String> releaseVersion = Optional.empty();
        // If no version exists use the version argument provided
        if (versionBehavior.isPresent()) {
            var versionBehaviorCode = CRMIReleaseVersionBehaviorCodes.fromCode(versionBehavior.get());
            if (existingVersion == null || existingVersion.isEmpty() || StringUtils.isBlank(existingVersion)) {
                return Optional.ofNullable(version);
            }
            String replaceDraftInExisting = existingVersion.replace("-draft", "");

            if (CRMIReleaseVersionBehaviorCodes.DEFAULT == versionBehaviorCode) {
                if (replaceDraftInExisting != null && !replaceDraftInExisting.isEmpty()) {
                    releaseVersion = Optional.of(replaceDraftInExisting);
                } else {
                    releaseVersion = Optional.ofNullable(version);
                }
            } else if (CRMIReleaseVersionBehaviorCodes.FORCE == versionBehaviorCode) {
                releaseVersion = Optional.ofNullable(version);
            } else if (CRMIReleaseVersionBehaviorCodes.CHECK == versionBehaviorCode) {
                if (!replaceDraftInExisting.equals(version)) {
                    throw new UnprocessableEntityException(String.format(
                            "versionBehavior specified is 'check' and the version provided ('%s') does not match the version currently specified on the root artifact ('%s').",
                            version, existingVersion));
                }
            }
        }
        return releaseVersion;
    }

    public static List<BundleEntryComponent> findArtifactCommentsToUpdate(
            MetadataResource rootArtifact, String releaseVersion, Repository repository) {
        List<BundleEntryComponent> returnEntries = new ArrayList<BundleEntryComponent>();
        // find any artifact assessments and update those as part of the bundle
        searchArtifactAssessmentForArtifact(rootArtifact.getIdElement(), repository).getEntry().stream()
                // The search is on Basic resources only unless we can register the ArtifactAssessment class
                .map(entry -> {
                    try {
                        return (Basic) entry.getResource();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(entry -> entry != null)
                // convert Basic to ArtifactAssessment by transferring the extensions
                .map(basic -> {
                    ArtifactAssessment extensionsTransferred = new ArtifactAssessment();
                    extensionsTransferred.setExtension(basic.getExtension());
                    extensionsTransferred.setId(basic.getClass().getSimpleName() + "/" + basic.getIdPart());
                    return extensionsTransferred;
                })
                .forEach(artifactComment -> {
                    artifactComment.setDerivedFromContentRelatedArtifact(
                            String.format("%s|%s", rootArtifact.getUrl(), releaseVersion));
                    returnEntries.add((BundleEntryComponent) PackageHelper.createEntry(artifactComment, true));
                });
        return returnEntries;
    }
}

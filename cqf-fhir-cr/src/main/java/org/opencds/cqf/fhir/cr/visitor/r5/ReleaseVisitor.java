package org.opencds.cqf.fhir.cr.visitor.r5;

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
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ArtifactAssessment;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r5.model.ResourceType;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.visitor.r5.CRMIReleaseExperimentalBehavior.CRMIReleaseExperimentalBehaviorCodes;
import org.opencds.cqf.fhir.cr.visitor.r5.CRMIReleaseVersionBehavior.CRMIReleaseVersionBehaviorCodes;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.slf4j.Logger;

public class ReleaseVisitor {

    private ReleaseVisitor() {}

    public static void checkNonExperimental(
            MetadataResource resource,
            CRMIReleaseExperimentalBehaviorCodes experimentalBehavior,
            Repository repository,
            Logger log)
            throws UnprocessableEntityException {
        if (CRMIReleaseExperimentalBehaviorCodes.NULL != experimentalBehavior
                && CRMIReleaseExperimentalBehaviorCodes.NONE != experimentalBehavior) {
            String nonExperimentalError =
                    "Root artifact is not Experimental, but references an Experimental resource with URL '%s'."
                            .formatted(resource.getUrl());
            if (CRMIReleaseExperimentalBehaviorCodes.WARN == experimentalBehavior && resource.getExperimental()) {
                log.warn(nonExperimentalError);
            } else if (CRMIReleaseExperimentalBehaviorCodes.ERROR == experimentalBehavior
                    && resource.getExperimental()) {
                throw new UnprocessableEntityException(nonExperimentalError);
            }
            // for ValueSets need to check recursively if any children are experimental since we don't own these
            if (resource.getResourceType().equals(ResourceType.ValueSet)) {
                var valueSet = (ValueSet) resource;
                List<IPrimitiveType<String>> valueSets = valueSet.getCompose().getInclude().stream()
                        .flatMap(include -> include.getValueSet().stream())
                        .collect(Collectors.toList());
                for (var value : valueSets) {
                    IKnowledgeArtifactAdapter.findLatestVersion(
                                    SearchHelper.searchRepositoryByCanonicalWithPaging(repository, value))
                            .ifPresent(childVs -> checkNonExperimental(
                                    (MetadataResource) childVs, experimentalBehavior, repository, log));
                }
            }
        }
    }

    public static void propagateEffectivePeriod(Period rootEffectivePeriod, IKnowledgeArtifactAdapter artifactAdapter) {
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
            var releaseLabelExtension = artifact.getExtensionByUrl(IKnowledgeArtifactAdapter.RELEASE_LABEL_URL);
            if (releaseLabelExtension == null) {
                // create the Extension and add it to the artifact if it doesn't exist
                releaseLabelExtension = new Extension(IKnowledgeArtifactAdapter.RELEASE_LABEL_URL);
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
        return repository.search(Bundle.class, ArtifactAssessment.class, searchParams);
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
            } else if (CRMIReleaseVersionBehaviorCodes.CHECK == versionBehaviorCode
                    && !replaceDraftInExisting.equals(version)) {
                throw new UnprocessableEntityException(
                        "versionBehavior specified is 'check' and the version provided ('%s') does not match the version currently specified on the root artifact ('%s')."
                                .formatted(version, existingVersion));
            }
        }
        return releaseVersion;
    }

    @SuppressWarnings("squid:S1612")
    public static List<BundleEntryComponent> findArtifactCommentsToUpdate(
            MetadataResource rootArtifact, String releaseVersion, Repository repository) {
        List<BundleEntryComponent> returnEntries = new ArrayList<>();
        // find any artifact assessments and update those as part of the bundle
        searchArtifactAssessmentForArtifact(rootArtifact.getIdElement(), repository).getEntry().stream()
                .map(entry -> (ArtifactAssessment) entry.getResource())
                .filter(entry -> entry != null)
                .forEach(artifactComment -> {
                    var ra = artifactComment.getContentFirstRep().addRelatedArtifact();
                    ra.setType(RelatedArtifactType.DERIVEDFROM)
                            .setResource("%s|%s".formatted(rootArtifact.getUrl(), releaseVersion));
                    returnEntries.add((BundleEntryComponent) PackageHelper.createEntry(artifactComment, true));
                });
        return returnEntries;
    }

    public static void extractDirectReferenceCodes(IKnowledgeArtifactAdapter rootAdapter, Measure measure) {
        Optional<Extension> effectiveDataRequirementsExt = measure.getExtension().stream()
                .filter(ext -> ext.getUrl().equals(Constants.CQFM_EFFECTIVE_DATA_REQUIREMENTS)
                        || ext.getUrl().equals(Constants.CRMI_EFFECTIVE_DATA_REQUIREMENTS))
                .findFirst();
        if (effectiveDataRequirementsExt.isPresent()) {
            Library effectiveDataRequirementsLib = null;
            if (effectiveDataRequirementsExt.get().getValue() instanceof Reference ref) {
                effectiveDataRequirementsLib = (Library) measure.getContained("#" + ref.getReference());
            } else if (effectiveDataRequirementsExt.get().getValue() instanceof CanonicalType canonicalType) {
                effectiveDataRequirementsLib = (Library) measure.getContained("#" + canonicalType.getCanonical());
            }
            if (effectiveDataRequirementsLib != null) {
                var proposedExtensions = effectiveDataRequirementsLib.getExtension().stream()
                    .filter(ext -> ext.getUrl().equals(Constants.CQFM_DIRECT_REFERENCE_EXTENSION))
                    .map(ext -> ext.setUrl(Constants.CQF_DIRECT_REFERENCE_EXTENSION)).toList();

                var existingRootAdapterExtensions =
                    rootAdapter.getExtension().stream().filter(ext ->
                        ext.getUrl().equals(Constants.CQFM_DIRECT_REFERENCE_EXTENSION)
                            || ext.getUrl().equals(Constants.CQF_DIRECT_REFERENCE_EXTENSION)).toList();

                for (var proposedExt : proposedExtensions) {
                    boolean shouldAddExtension = true;
                    Coding proposedCoding = (Coding)proposedExt.getValue();
                    for (var existingExt : existingRootAdapterExtensions) {
                        Coding existingCoding = (Coding)existingExt.getValue();
                        boolean systemMatches = proposedCoding.getSystem().equals(existingCoding.getSystem());
                        boolean codeMatches = proposedCoding.getCode().equals(existingCoding.getCode());
                        boolean versionMatches = proposedCoding.getVersion() == null || proposedCoding.getVersion().equals(existingCoding.getVersion());

                        if (systemMatches && codeMatches && versionMatches) {
                            shouldAddExtension = false;
                            break;
                        }
                    }

                    if (shouldAddExtension) {
                        rootAdapter.addExtension(proposedExt);
                    }
                }
            }
        }
    }
}

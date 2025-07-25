package org.opencds.cqf.fhir.cr.visitor.r5;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.ArtifactAssessment;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r5.model.ResourceType;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.ValueSet;
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
            IRepository repository,
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

    public static Bundle searchArtifactAssessmentForArtifact(IIdType reference, IRepository repository) {
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
            MetadataResource rootArtifact, String releaseVersion, IRepository repository) {
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
        Library effectiveDataRequirementsLib = getEffectiveDataRequirementsLib(measure);

        if (effectiveDataRequirementsLib != null) {
            var proposedExtensions = effectiveDataRequirementsLib.getExtension().stream()
                    .filter(ext -> ext.getUrl().equals(Constants.CQF_DIRECT_REFERENCE_EXTENSION))
                    .toList();

            var existingRootAdapterExtensions = rootAdapter.getExtension().stream()
                    .filter(ext -> ext.getUrl().equals(Constants.CQFM_DIRECT_REFERENCE_EXTENSION)
                            || ext.getUrl().equals(Constants.CQF_DIRECT_REFERENCE_EXTENSION))
                    .toList();

            var expansionParams = rootAdapter.getExpansionParameters().map(p -> ((Parameters) p));

            for (var proposedExt : proposedExtensions) {
                boolean shouldAddExtension = true;
                Coding proposedCoding = (Coding) proposedExt.getValue();

                setCodeSystemVersion(proposedCoding, expansionParams.orElse(null));

                for (var existingExt : existingRootAdapterExtensions) {
                    Coding existingCoding = (Coding) existingExt.getValue();

                    if (codingsMatch(proposedCoding, existingCoding)) {
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

    private static Library getEffectiveDataRequirementsLib(Measure measure) {
        Optional<Extension> effectiveDataRequirementsExt = measure.getExtension().stream()
                .filter(ext -> ext.getUrl().equals(Constants.CQFM_EFFECTIVE_DATA_REQUIREMENTS)
                        || ext.getUrl().equals(Constants.CRMI_EFFECTIVE_DATA_REQUIREMENTS))
                .findFirst();
        if (effectiveDataRequirementsExt.isPresent()) {
            if (effectiveDataRequirementsExt.get().getValue() instanceof Reference ref) {
                return (Library) measure.getContained(ref.getReference());
            } else if (effectiveDataRequirementsExt.get().getValue() instanceof CanonicalType canonicalType) {
                return (Library) measure.getContained(canonicalType.asStringValue());
            }
            return null;
        }
        return null;
    }

    private static boolean codingsMatch(Coding proposedCoding, Coding existingCoding) {
        boolean systemMatches = proposedCoding.getSystem().equals(existingCoding.getSystem());
        boolean codeMatches = proposedCoding.getCode().equals(existingCoding.getCode());
        boolean versionMatches = proposedCoding.getVersion() == null
                || proposedCoding.getVersion().equals(existingCoding.getVersion());

        return systemMatches && codeMatches && versionMatches;
    }

    public static void captureInputExpansionParams(
            IBaseParameters inputExpansionParams, IKnowledgeArtifactAdapter rootAdapter) {
        if (inputExpansionParams != null) {
            if (inputExpansionParams instanceof Parameters parameters) {
                // Deep copy of inputParameters
                Parameters inputParametersCopy = parameters.copy();
                inputParametersCopy.setId("input-exp-params");
                var inputExpansionParametersExtension =
                        new Extension(Constants.CQF_INPUT_EXPANSION_PARAMETERS, new Reference("#input-exp-params"));
                rootAdapter.addExtension(inputExpansionParametersExtension);
                ((DomainResource) rootAdapter.get()).addContained(inputParametersCopy);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported IBaseParameters implementation: " + inputExpansionParams.getClass());
            }
        }
    }

    private static void setCodeSystemVersion(Coding proposedCoding, Parameters expansionParams) {
        List<UriType> systemVersions = new ArrayList<>();
        if (expansionParams != null) {
            systemVersions = expansionParams.getParameter().stream()
                    .filter(param -> param.getName().equals(Constants.SYSTEM_VERSION))
                    .map(sysVerParam -> (UriType) sysVerParam.getValue())
                    .toList();
        }
        if (proposedCoding.getVersion() == null && !systemVersions.isEmpty()) {
            for (var sysVer : systemVersions) {
                var idParts = sysVer.getValue().split("\\|");
                if (idParts[0].equals(proposedCoding.getSystem())) {
                    proposedCoding.setVersion(idParts[1]);
                    break;
                }
            }
        }
    }
}

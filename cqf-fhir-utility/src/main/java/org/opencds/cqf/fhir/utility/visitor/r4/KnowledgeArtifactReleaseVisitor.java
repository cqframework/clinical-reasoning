package org.opencds.cqf.fhir.utility.visitor.r4;

import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.r4.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.r4.CRMIReleaseExperimentalBehavior.CRMIReleaseExperimentalBehaviorCodes;
import org.opencds.cqf.fhir.utility.r4.CRMIReleaseVersionBehavior.CRMIReleaseVersionBehaviorCodes;
import org.slf4j.LoggerFactory;
import  org.opencds.cqf.fhir.utility.r4.ArtifactAssessment;
import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;
import org.opencds.cqf.fhir.utility.r4.PackageHelper;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static java.util.Comparator.comparing;
import org.slf4j.Logger;

public class KnowledgeArtifactReleaseVisitor implements r4KnowledgeArtifactVisitor {
	private Logger log = LoggerFactory.getLogger(KnowledgeArtifactReleaseVisitor.class);

  public IBase visit(r4LibraryAdapter rootLibraryAdapter, Repository repository, Parameters operationParameters) {
    boolean latestFromTxServer = ((Parameters)operationParameters).getParameterBool("latestFromTxServer");
    
    String version = MetadataResourceHelper.getParameter("version", operationParameters, StringType.class).map(t -> t.getValue()).orElseThrow(() -> new UnprocessableEntityException("Version must be present"));
    String releaseLabel = MetadataResourceHelper.getParameter("releaseLabel", operationParameters, StringType.class).map(t -> t.getValue()).orElse("");
    Optional<CodeType> versionBehavior = MetadataResourceHelper.getParameter("versionBehavior", operationParameters, CodeType.class);
    Optional<CodeType> requireNonExpermimental = MetadataResourceHelper.getParameter("requireNonExperimental", operationParameters, CodeType.class);
    CRMIReleaseVersionBehaviorCodes versionBehaviorCode;
    CRMIReleaseExperimentalBehaviorCodes experimentalBehaviorCode;
    try {
        versionBehaviorCode = versionBehavior.isPresent() ? CRMIReleaseVersionBehaviorCodes.fromCode(versionBehavior.get().getCode()) : CRMIReleaseVersionBehaviorCodes.NULL;
        experimentalBehaviorCode = requireNonExpermimental.isPresent() ? CRMIReleaseExperimentalBehaviorCodes.fromCode(requireNonExpermimental.get().getCode()) : CRMIReleaseExperimentalBehaviorCodes.NULL;
    } catch (FHIRException e) {
        throw new UnprocessableEntityException(e.getMessage());
    }
// TODO: This check is to avoid partial releases and should be removed once the argument is supported.
    if (latestFromTxServer) {
        throw new NotImplementedOperationException("Support for 'latestFromTxServer' is not yet implemented.");
    }
    checkReleaseVersion(version, versionBehaviorCode);
    Library rootLibrary = rootLibraryAdapter.get();
    Date currentApprovalDate = rootLibraryAdapter.getApprovalDate();
    checkReleasePreconditions(rootLibrary, currentApprovalDate);

    // Determine which version should be used.
    String existingVersion = rootLibrary.hasVersion() ? rootLibrary.getVersion().replace("-draft","") : null;
    String releaseVersion = getReleaseVersion(version, versionBehaviorCode, existingVersion)
        .orElseThrow(() -> new UnprocessableEntityException("Could not resolve a version for the root artifact."));
    Period rootEffectivePeriod = (Period) rootLibraryAdapter.getEffectivePeriod();
    // if the root artifact is experimental then we don't need to check for experimental children
    if (rootLibrary.getExperimental()) {
        experimentalBehaviorCode = CRMIReleaseExperimentalBehaviorCodes.NONE;
    }
    List<MetadataResource> releasedResources = internalRelease(rootLibraryAdapter, releaseVersion, rootEffectivePeriod, versionBehaviorCode, latestFromTxServer, experimentalBehaviorCode, repository);
    updateReleaseLabel(rootLibrary, releaseLabel);
    List<DependencyInfo> rootArtifactOriginalDependencies = new ArrayList<DependencyInfo>(rootLibraryAdapter.getDependencies());
    // Get list of extensions which need to be preserved
    List<DependencyInfo> originalDependenciesWithExtensions = rootArtifactOriginalDependencies.stream().filter(dep -> dep.getExtension() != null && dep.getExtension().size() > 0).collect(Collectors.toList());
    // once iteration is complete, delete all depends-on RAs in the root artifact
    rootLibraryAdapter.getRelatedArtifact().removeIf(ra -> ((RelatedArtifact)ra).getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON);

    Bundle transactionBundle = new Bundle()
        .setType(Bundle.BundleType.TRANSACTION);
    for (MetadataResource artifact: releasedResources) {
        transactionBundle.addEntry(PackageHelper.createEntry(artifact, true));

        r4KnowledgeArtifactAdapter artifactAdapter = new AdapterFactory().createKnowledgeArtifactAdapter(artifact);
        List<RelatedArtifact> components = artifactAdapter.getComponents();
        // add all root artifact components and child artifact components recursively as root artifact dependencies
        for (RelatedArtifact component : components) {
            MetadataResource resource;
            // if the relatedArtifact is Owned, need to update the reference to the new Version
            if (r4KnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(component)) {
                resource = checkIfReferenceInList(component, releasedResources)
                // should never happen since we check all references as part of `internalRelease`
                .orElseThrow(() -> new InternalErrorException("Owned resource reference not found during release"));
                String reference = String.format("%s|%s", resource.getUrl(), resource.getVersion());
                component.setResource(reference);
            } else if (Canonicals.getVersion(component.getResourceElement()) == null || Canonicals.getVersion(component.getResourceElement()).isEmpty()) {
                // if the not Owned component doesn't have a version, try to find the latest version
                String updatedReference = tryUpdateReferenceToLatestActiveVersion(component.getResource(), repository, artifact.getUrl());
                component.setResource(updatedReference);
            }
            RelatedArtifact componentToDependency = new RelatedArtifact().setType(RelatedArtifact.RelatedArtifactType.DEPENDSON).setResource(component.getResourceElement().getValueAsString());
            rootLibraryAdapter.getRelatedArtifact().add(componentToDependency);
        }

        List<DependencyInfo> dependencies = artifactAdapter.getDependencies();
        for (DependencyInfo dependency : dependencies) {
            // if the dependency gets updated as part of $release then update the reference as well
            Optional<MetadataResource> maybeReference = checkIfReferenceInList(dependency, releasedResources);
            if (maybeReference.isPresent()) {
                String updatedReference = String.format("%s|%s", maybeReference.get().getUrl(), maybeReference.get().getVersion());
                dependency.setReference(updatedReference);
            } else {
                if (Canonicals.getVersion(dependency.getReference()) == null || Canonicals.getVersion(dependency.getReference()).isEmpty()) {
                    // TODO: update when we support expansionParameters and requireVersionedDependencies
                    String updatedReference = tryUpdateReferenceToLatestActiveVersion(dependency.getReference(), repository, artifact.getUrl());
                    dependency.setReference(updatedReference);
                }
            }
            // only add the dependency to the manifest if it is from a leaf artifact
            if (!artifact.getUrl().equals(rootLibrary.getUrl())) {
                RelatedArtifact newDep = new RelatedArtifact();
                newDep
                    .setType(RelatedArtifactType.DEPENDSON)
                    .setResource(dependency.getReference());
                rootLibraryAdapter.getRelatedArtifact().add(newDep);
            }
        }
    }
    // removed duplicates and add
    List<RelatedArtifact> distinctResolvedRelatedArtifacts = new ArrayList<>();
    for (RelatedArtifact resolvedRelatedArtifact: rootLibraryAdapter.getRelatedArtifact()) {
        boolean isDistinct = !distinctResolvedRelatedArtifacts.stream().anyMatch(distinctRelatedArtifact -> {
            boolean referenceNotInArray = distinctRelatedArtifact.getResource().equals(resolvedRelatedArtifact.getResource());
            boolean typeMatches = distinctRelatedArtifact.getType().equals(resolvedRelatedArtifact.getType());
            return referenceNotInArray && typeMatches;
        });
        if (isDistinct) {
            distinctResolvedRelatedArtifacts.add(resolvedRelatedArtifact);
            // preserve Extensions if found
            originalDependenciesWithExtensions
            .stream()
                .filter(originalDep -> originalDep.getReference().equals(resolvedRelatedArtifact.getResource()))
                .findFirst()
                .ifPresent(dep -> {
                    checkIfValueSetNeedsCondition(null, dep, repository);
                    resolvedRelatedArtifact.getExtension().addAll(dep.getExtension().stream().map(e -> (Extension)e).collect(Collectors.toList()));
                    originalDependenciesWithExtensions.removeIf(ra -> ra.getReference().equals(resolvedRelatedArtifact.getResource()));
                });
        }
    }
    // update ArtifactComments referencing the old Canonical Reference
    transactionBundle.getEntry().addAll(findArtifactCommentsToUpdate(rootLibrary, releaseVersion, repository));
    rootLibraryAdapter.setRelatedArtifact(distinctResolvedRelatedArtifacts);

    return repository.transaction(transactionBundle);

  }
  private void checkIfValueSetNeedsCondition(MetadataResource resource, DependencyInfo relatedArtifact, Repository repository) throws UnprocessableEntityException {
    if (resource == null 
    && relatedArtifact != null 
    && relatedArtifact.getReference() != null 
    && Canonicals.getResourceType(relatedArtifact.getReference()).equals("ValueSet")) {
        List<MetadataResource> searchResults = getResourcesFromBundle((Bundle) SearchHelper.searchRepositoryByCanonicalWithPaging( repository, new CanonicalType(relatedArtifact.getReference())));
        if (searchResults.size() > 0) {
            resource = searchResults.get(0);
        }
    }
    if (resource != null && resource.getResourceType() == ResourceType.ValueSet) {
        ValueSet valueSet = (ValueSet)resource;
        boolean isLeaf = !valueSet.hasCompose() || (valueSet.hasCompose() && valueSet.getCompose().getIncludeFirstRep().getValueSet().size() == 0);
        Optional<? extends IBaseExtension> maybeConditionExtension = Optional.ofNullable(relatedArtifact)
            .map(DependencyInfo::getExtension)
            .map(list -> {
                return list.stream().filter(ext -> ext.getUrl().equalsIgnoreCase(IBaseKnowledgeArtifactAdapter.valueSetConditionUrl)).findFirst().orElse(null);
            });
        if (isLeaf && !maybeConditionExtension.isPresent()) {
            throw new UnprocessableEntityException("Missing condition on ValueSet : " + valueSet.getUrl());
        }
    }
}
  private List<MetadataResource> internalRelease(r4KnowledgeArtifactAdapter artifactAdapter, String version, Period rootEffectivePeriod,
																 CRMIReleaseVersionBehaviorCodes versionBehavior, boolean latestFromTxServer, CRMIReleaseExperimentalBehaviorCodes experimentalBehavior, Repository repository) throws NotImplementedOperationException, ResourceNotFoundException {
		List<MetadataResource> resourcesToUpdate = new ArrayList<MetadataResource>();

		// Step 1: Update the Date and the version
		// Need to update the Date element because we're changing the status
		artifactAdapter.get().setDate(new Date());
		artifactAdapter.get().setStatus(Enumerations.PublicationStatus.ACTIVE);
		artifactAdapter.get().setVersion(version);

		// Step 2: propagate effectivePeriod if it doesn't exist
		Period effectivePeriod = (Period) artifactAdapter.getEffectivePeriod();
		// if the root artifact period is NOT null AND HAS a start or an end date
		if((rootEffectivePeriod != null && (rootEffectivePeriod.hasStart() || rootEffectivePeriod.hasEnd()))
		// and the current artifact period IS null OR does NOT HAVE a start or an end date
		&& (effectivePeriod == null || !(effectivePeriod.hasStart() || effectivePeriod.hasEnd()))){
			artifactAdapter.setEffectivePeriod(rootEffectivePeriod);
		}

		resourcesToUpdate.add(artifactAdapter.get());

		// Step 3 : Get all the OWNED relatedArtifacts
		for (RelatedArtifact ownedRelatedArtifact : artifactAdapter.getOwnedRelatedArtifacts()) {
			if (ownedRelatedArtifact.hasResource()) {
				MetadataResource referencedResource;
				CanonicalType ownedResourceReference = ownedRelatedArtifact.getResourceElement();
				Boolean alreadyUpdated = resourcesToUpdate
					.stream()
					.filter(r -> r.getUrl().equals(Canonicals.getUrl(ownedResourceReference)))
					.findAny()
					.isPresent();
				if(!alreadyUpdated) {
					// For composition references, if a version is not specified in the reference then the latest version
					// of the referenced artifact should be used. If a version is specified then `searchRepositoryByCanonicalWithPaging` will
					// return that version.
					referencedResource = KnowledgeArtifactAdapter.findLatestVersion((Bundle) SearchHelper.searchRepositoryByCanonicalWithPaging(repository, ownedResourceReference ))
					.orElseThrow(()-> new ResourceNotFoundException(
							String.format("Resource with URL '%s' is Owned by this repository and referenced by resource '%s', but was not found on the server.",
								ownedResourceReference.getValueAsString(),
								artifactAdapter.get().getUrl()))
					);
					r4KnowledgeArtifactAdapter searchResultAdapter = new AdapterFactory().createKnowledgeArtifactAdapter(referencedResource);
            
					if (CRMIReleaseExperimentalBehaviorCodes.NULL != experimentalBehavior && CRMIReleaseExperimentalBehaviorCodes.NONE != experimentalBehavior) {
						checkNonExperimental(referencedResource, experimentalBehavior, repository);
					}
					resourcesToUpdate.addAll(internalRelease(searchResultAdapter, version, rootEffectivePeriod, versionBehavior, latestFromTxServer, experimentalBehavior, repository));
				}
			}
		}

		return resourcesToUpdate;
	}
    private void checkNonExperimental(MetadataResource resource, CRMIReleaseExperimentalBehaviorCodes experimentalBehavior, Repository repository) throws UnprocessableEntityException {
		String nonExperimentalError = String.format("Root artifact is not Experimental, but references an Experimental resource with URL '%s'.",
								resource.getUrl());
		if (CRMIReleaseExperimentalBehaviorCodes.WARN == experimentalBehavior && resource.getExperimental()) {
			log.warn(nonExperimentalError);
		} else if (CRMIReleaseExperimentalBehaviorCodes.ERROR == experimentalBehavior && resource.getExperimental()) {
			throw new UnprocessableEntityException(nonExperimentalError);
		}
		// for ValueSets need to check recursively if any chldren are experimental since we don't own these
		if (resource.getResourceType().equals(ResourceType.ValueSet)) {
			ValueSet valueSet = (ValueSet) resource;
			List<CanonicalType> valueSets = valueSet
				.getCompose()
				.getInclude()
				.stream().flatMap(include -> include.getValueSet().stream())
				.collect(Collectors.toList());
			for (CanonicalType value: valueSets) {
				KnowledgeArtifactAdapter.findLatestVersion((Bundle) SearchHelper.searchRepositoryByCanonicalWithPaging(repository, value))
				.ifPresent(childVs -> checkNonExperimental(childVs, experimentalBehavior, repository));
			}
		}
	}
  private Optional<String> getReleaseVersion(String version, CRMIReleaseVersionBehaviorCodes versionBehavior, String existingVersion) throws UnprocessableEntityException {
    Optional<String> releaseVersion = Optional.ofNullable(null);
    // If no version exists use the version argument provided
    if (existingVersion == null || existingVersion.isEmpty() || StringUtils.isBlank(existingVersion)) {
        return Optional.ofNullable(version);
    }
    String replaceDraftInExisting = existingVersion.replace("-draft","");

    if (CRMIReleaseVersionBehaviorCodes.DEFAULT == versionBehavior) {
        if(replaceDraftInExisting != null && !replaceDraftInExisting.isEmpty()) {
            releaseVersion = Optional.of(replaceDraftInExisting);
        } else {
            releaseVersion = Optional.ofNullable(version);
        }
    } else if (CRMIReleaseVersionBehaviorCodes.FORCE == versionBehavior) {
        releaseVersion = Optional.ofNullable(version);
    } else if (CRMIReleaseVersionBehaviorCodes.CHECK == versionBehavior) {
        if (!replaceDraftInExisting.equals(version)) {
            throw new UnprocessableEntityException(String.format("versionBehavior specified is 'check' and the version provided ('%s') does not match the version currently specified on the root artifact ('%s').",version,existingVersion));
        }
    }
    return releaseVersion;
}
private void updateReleaseLabel(MetadataResource artifact, String releaseLabel) throws IllegalArgumentException {
    if (releaseLabel != null) {
        Extension releaseLabelExtension = artifact.getExtensionByUrl(IBaseKnowledgeArtifactAdapter.releaseLabelUrl);
        if (releaseLabelExtension == null) {
            // create the Extension and add it to the artifact if it doesn't exist
            releaseLabelExtension = new Extension(IBaseKnowledgeArtifactAdapter.releaseLabelUrl);
            artifact.addExtension(releaseLabelExtension);
        }
        releaseLabelExtension.setValue(new StringType(releaseLabel));
    }
}
  private Optional<MetadataResource> checkIfReferenceInList(RelatedArtifact artifactToUpdate, List<MetadataResource> resourceList){
    Optional<MetadataResource> updatedReference = Optional.ofNullable(null);
    for (MetadataResource resource : resourceList) {
        String referenceURL = Canonicals.getUrl(artifactToUpdate.getResource());
        String currentResourceURL = resource.getUrl();
        if (referenceURL.equals(currentResourceURL)) {
            return Optional.of(resource);
        }
    }
    return updatedReference;
}
private Optional<MetadataResource> checkIfReferenceInList(DependencyInfo artifactToUpdate, List<MetadataResource> resourceList){
    Optional<MetadataResource> updatedReference = Optional.ofNullable(null);
    for (MetadataResource resource : resourceList) {
        String referenceURL = Canonicals.getUrl(artifactToUpdate.getReference());
        String currentResourceURL = resource.getUrl();
        if (referenceURL.equals(currentResourceURL)) {
            return Optional.of(resource);
        }
    }
    return updatedReference;
}
  private void checkReleasePreconditions(MetadataResource artifact, Date approvalDate) throws PreconditionFailedException {
		if (artifact == null) {
			throw new ResourceNotFoundException("Resource not found.");
		}

		if (Enumerations.PublicationStatus.DRAFT != artifact.getStatus()) {
			throw new PreconditionFailedException(String.format("Resource with ID: '%s' does not have a status of 'draft'.", artifact.getIdElement().getIdPart()));
		}
		if (approvalDate == null) {
			throw new PreconditionFailedException(String.format("The artifact must be approved (indicated by approvalDate) before it is eligible for release."));
		}
		if (approvalDate.before(artifact.getDate())) {
			throw new PreconditionFailedException(
				String.format("The artifact was approved on '%s', but was last modified on '%s'. An approval must be provided after the most-recent update.", approvalDate, artifact.getDate()));
		}
	}
	
  private Bundle searchArtifactAssessmentForArtifact(IdType reference, Repository repository) {
		Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
		List<IQueryParameterType> urlList = new ArrayList<>();
		urlList.add(new ReferenceParam(reference));
		searchParams.put("artifact", urlList);
		Bundle searchResultsBundle = (Bundle)repository.search(Bundle.class,Basic.class, searchParams);
		return searchResultsBundle;
	}
  private List<BundleEntryComponent> findArtifactCommentsToUpdate(MetadataResource rootArtifact,String releaseVersion, Repository repository){
		List<BundleEntryComponent> returnEntries = new ArrayList<BundleEntryComponent>();
		// find any artifact assessments and update those as part of the bundle
		this.searchArtifactAssessmentForArtifact(rootArtifact.getIdElement(), repository)
			.getEntry()
			.stream()
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
				artifactComment.setDerivedFromContentRelatedArtifact(new CanonicalType(String.format("%s|%s", rootArtifact.getUrl(), releaseVersion)));
				returnEntries.add(PackageHelper.createEntry(artifactComment, true));
			});
			return returnEntries;
	}
  private void checkReleaseVersion(String version,CRMIReleaseVersionBehaviorCodes versionBehavior) throws UnprocessableEntityException {
    if (CRMIReleaseVersionBehaviorCodes.NULL == versionBehavior) {
        throw new UnprocessableEntityException("'versionBehavior' must be provided as an argument to the $release operation. Valid values are 'default', 'check', 'force'.");
    }
    checkVersionValidSemver(version);
}

	
private String tryUpdateReferenceToLatestActiveVersion(String inputReference, Repository repository, String sourceArtifactUrl) throws ResourceNotFoundException {
		List<MetadataResource> matchingResources = getResourcesFromBundle((Bundle) SearchHelper.searchRepositoryByCanonicalWithPaging(repository, new CanonicalType(inputReference)))
			.stream()
			.filter(r -> r.getStatus().equals(Enumerations.PublicationStatus.ACTIVE))
			.collect(Collectors.toList());

		if (matchingResources.isEmpty()) {
			return inputReference;
		} else {
			// TODO: Log which version was selected
			matchingResources.sort(comparing(r -> ((MetadataResource) r).getVersion()).reversed());
			MetadataResource latestActiveVersion = matchingResources.get(0);
			String latestActiveReference = String.format("%s|%s", latestActiveVersion.getUrl(), latestActiveVersion.getVersion());
			return latestActiveReference;
		}
	}
	private void checkVersionValidSemver(String version) throws UnprocessableEntityException {
		if (version == null || version.isEmpty()) {
			throw new UnprocessableEntityException("The version argument is required");
		}
		if (version.contains("draft")) {
			throw new UnprocessableEntityException("The version cannot contain 'draft'");
		}
		if (version.contains("/") || version.contains("\\") || version.contains("|")) {
			throw new UnprocessableEntityException("The version contains illegal characters");
		}
		Pattern pattern = Pattern.compile("^(\\d+\\.)(\\d+\\.)(\\d+\\.)?(\\*|\\d+)$", Pattern.CASE_INSENSITIVE);
    	Matcher matcher = pattern.matcher(version);
    	boolean matchFound = matcher.find();
		if (!matchFound) {
			throw new UnprocessableEntityException("The version must be in the format MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH.REVISION");
		}
	}
	private List<MetadataResource> getResourcesFromBundle(Bundle bundle) {
		List<MetadataResource> resourceList = new ArrayList<>();

		if (!bundle.getEntryFirstRep().isEmpty()) {
			List<Bundle.BundleEntryComponent> referencedResourceEntries = bundle.getEntry();
			for (Bundle.BundleEntryComponent entry: referencedResourceEntries) {
				if (entry.hasResource() && entry.getResource() instanceof MetadataResource) {
					MetadataResource referencedResource = (MetadataResource) entry.getResource();
					resourceList.add(referencedResource);
				}
			}
		}

		return resourceList;
	}
  public IBase visit(IBasePlanDefinitionAdapter planDefinition, Repository repository, Parameters operationParameters) {
    List<DependencyInfo> dependencies = planDefinition.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }

  public IBase visit(IBasePlanDefinitionAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
    List<DependencyInfo> dependencies = valueSet.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }

  public IBase visit(ValueSetAdapter valueSet, Repository repository, Parameters operationParameters) {
    List<DependencyInfo> dependencies = valueSet.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }

  public IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
    List<DependencyInfo> dependencies = valueSet.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }

  public IBase visit(IBaseKnowledgeArtifactAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
    List<DependencyInfo> dependencies = valueSet.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }

  public IBase visit(IBaseLibraryAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
    List<DependencyInfo> dependencies = valueSet.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }
}
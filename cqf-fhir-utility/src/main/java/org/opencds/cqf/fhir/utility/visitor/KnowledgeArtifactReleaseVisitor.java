package org.opencds.cqf.fhir.utility.visitor;

import static java.util.Comparator.comparing;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnowledgeArtifactReleaseVisitor implements KnowledgeArtifactVisitor {
    private Logger log = LoggerFactory.getLogger(KnowledgeArtifactReleaseVisitor.class);

    @SuppressWarnings("unchecked")
    @Override
    public IBase visit(LibraryAdapter rootLibraryAdapter, Repository repository, IBaseParameters operationParameters) {
        // boolean latestFromTxServer = operationParameters.getParameterBool("latestFromTxServer");
        Optional<Boolean> latestFromTxServer = VisitorHelper.getParameter(
                        "latestFromTxServer", operationParameters, IPrimitiveType.class)
                .map(t -> (Boolean) t.getValue());
        // TODO: This check is to avoid partial releases and should be removed once the argument is supported.
        if (latestFromTxServer.isPresent()) {
            throw new NotImplementedOperationException("Support for 'latestFromTxServer' is not yet implemented.");
        }
        String version = VisitorHelper.getParameter("version", operationParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue())
                .orElseThrow(() -> new UnprocessableEntityException("Version must be present"));
        String releaseLabel = VisitorHelper.getParameter("releaseLabel", operationParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue())
                .orElse("");
        Optional<String> versionBehavior = VisitorHelper.getParameter(
                        "versionBehavior", operationParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue());
        Optional<String> requireNonExpermimental = VisitorHelper.getParameter(
                        "requireNonExperimental", operationParameters, IPrimitiveType.class)
                .map(t -> (String) t.getValue());
        checkReleaseVersion(version, versionBehavior);
        var rootLibrary = rootLibraryAdapter.get();
        var fhirVersion = rootLibrary.getStructureFhirVersionEnum();
        var currentApprovalDate = rootLibraryAdapter.getApprovalDate();
        checkReleasePreconditions(rootLibraryAdapter, currentApprovalDate);

        // Determine which version should be used.
        var existingVersion = rootLibraryAdapter.hasVersion()
                ? rootLibraryAdapter.getVersion().replace("-draft", "")
                : null;
        var releaseVersion = getReleaseVersion(version, versionBehavior, existingVersion, fhirVersion)
                .orElseThrow(
                        () -> new UnprocessableEntityException("Could not resolve a version for the root artifact."));
        var rootEffectivePeriod = rootLibraryAdapter.getEffectivePeriod();
        // if the root artifact is experimental then we don't need to check for experimental children
        if (rootLibraryAdapter.getExperimental()) {
            requireNonExpermimental = Optional.of("none");
        }
        var releasedResources = internalRelease(
                rootLibraryAdapter,
                releaseVersion,
                rootEffectivePeriod,
                latestFromTxServer.orElse(false),
                requireNonExpermimental,
                repository);
        updateReleaseLabel(rootLibrary, releaseLabel);
        var rootArtifactOriginalDependencies = new ArrayList<IDependencyInfo>(rootLibraryAdapter.getDependencies());
        // Get list of extensions which need to be preserved
        var originalDependenciesWithExtensions = rootArtifactOriginalDependencies.stream()
                .filter(dep -> dep.getExtension() != null && dep.getExtension().size() > 0)
                .collect(Collectors.toList());
        // once iteration is complete, delete all depends-on RAs in the root artifact
        rootLibraryAdapter.getRelatedArtifact().removeIf(ra -> KnowledgeArtifactAdapter.getRelatedArtifactType(ra)
                .equalsIgnoreCase("depends-on"));

        var transactionBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");
        for (var artifact : releasedResources) {
            var entry = PackageHelper.createEntry(artifact, true);
            BundleHelper.addEntry(transactionBundle, entry);
            var artifactAdapter = AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(artifact);
            var components = artifactAdapter.getComponents();
            // add all root artifact components and child artifact components recursively as root artifact dependencies
            for (var component : components) {
                IDomainResource resource;
                var relatedArtifactReference = KnowledgeArtifactAdapter.getRelatedArtifactReference(component);
                // if the relatedArtifact is Owned, need to update the reference to the new Version
                if (KnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(component)) {
                    resource = checkIfReferenceInList(relatedArtifactReference, releasedResources)
                            // should never happen since we check all references as part of `internalRelease`
                            .orElseThrow(() ->
                                    new InternalErrorException("Owned resource reference not found during release"));
                    var adapter = AdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum())
                            .createKnowledgeArtifactAdapter(resource);
                    String reference = String.format("%s|%s", adapter.getUrl(), adapter.getVersion());
                    KnowledgeArtifactAdapter.setRelatedArtifactReference(component, reference);
                } else if (Canonicals.getVersion(relatedArtifactReference) == null
                        || Canonicals.getVersion(relatedArtifactReference).isEmpty()) {
                    // if the not Owned component doesn't have a version, try to find the latest version
                    String updatedReference = tryUpdateReferenceToLatestActiveVersion(
                            relatedArtifactReference, repository, artifactAdapter.getUrl());
                    KnowledgeArtifactAdapter.setRelatedArtifactReference(component, updatedReference);
                }
                var componentToDependency = KnowledgeArtifactAdapter.newRelatedArtifact(
                        fhirVersion, "depends-on", relatedArtifactReference);
                rootLibraryAdapter.getRelatedArtifact().add(componentToDependency);
            }

            var dependencies = artifactAdapter.getDependencies();
            for (var dependency : dependencies) {
                // if the dependency gets updated as part of $release then update the reference as well
                var maybeReference = checkIfReferenceInList(dependency, releasedResources);
                if (maybeReference.isPresent()) {
                    var adapter = AdapterFactory.forFhirVersion(fhirVersion)
                            .createKnowledgeArtifactAdapter(maybeReference.get());
                    String updatedReference = adapter.hasVersion()
                            ? String.format("%s|%s", adapter.getUrl(), adapter.getVersion())
                            : adapter.getUrl();
                    dependency.setReference(updatedReference);
                } else {
                    if (Canonicals.getVersion(dependency.getReference()) == null
                            || Canonicals.getVersion(dependency.getReference()).isEmpty()) {
                        // TODO: update when we support expansionParameters and requireVersionedDependencies
                        String updatedReference = tryUpdateReferenceToLatestActiveVersion(
                                dependency.getReference(), repository, artifactAdapter.getUrl());
                        dependency.setReference(updatedReference);
                    }
                }
                // only add the dependency to the manifest if it is from a leaf artifact
                if (!artifactAdapter.getUrl().equals(rootLibraryAdapter.getUrl())) {
                    var newDep = KnowledgeArtifactAdapter.newRelatedArtifact(
                            fhirVersion, "depends-on", dependency.getReference());
                    rootLibraryAdapter.getRelatedArtifact().add(newDep);
                }
            }
        }
        // removed duplicates and add
        var relatedArtifacts = rootLibraryAdapter.getRelatedArtifact();
        var distinctResolvedRelatedArtifacts = new ArrayList<>(relatedArtifacts);
        distinctResolvedRelatedArtifacts.clear();
        for (var resolvedRelatedArtifact : relatedArtifacts) {
            var relatedArtifactReference =
                    KnowledgeArtifactAdapter.getRelatedArtifactReference(resolvedRelatedArtifact);
            boolean isDistinct = !distinctResolvedRelatedArtifacts.stream().anyMatch(distinctRelatedArtifact -> {
                boolean referenceNotInArray = relatedArtifactReference.equals(
                        KnowledgeArtifactAdapter.getRelatedArtifactReference(distinctRelatedArtifact));
                boolean typeMatches = KnowledgeArtifactAdapter.getRelatedArtifactType(distinctRelatedArtifact)
                        .equals(KnowledgeArtifactAdapter.getRelatedArtifactType(resolvedRelatedArtifact));
                return referenceNotInArray && typeMatches;
            });
            if (isDistinct) {
                distinctResolvedRelatedArtifacts.add(resolvedRelatedArtifact);
                // preserve Extensions if found
                originalDependenciesWithExtensions.stream()
                        .filter(originalDep -> originalDep.getReference().equals(relatedArtifactReference))
                        .findFirst()
                        .ifPresent(dep -> {
                            ((List<IBaseExtension<?, ?>>) resolvedRelatedArtifact.getExtension())
                                    .addAll((List<IBaseExtension<?, ?>>) dep.getExtension());
                            originalDependenciesWithExtensions.removeIf(
                                    ra -> ra.getReference().equals(relatedArtifactReference));
                        });
            }
        }
        // update ArtifactComments referencing the old Canonical Reference
        findArtifactCommentsToUpdate(rootLibrary, releaseVersion, repository).forEach(entry -> {
            BundleHelper.addEntry(transactionBundle, entry);
        });
        rootLibraryAdapter.setRelatedArtifact(distinctResolvedRelatedArtifacts);

        return repository.transaction(transactionBundle);
    }

    private List<IDomainResource> internalRelease(
            KnowledgeArtifactAdapter artifactAdapter,
            String version,
            ICompositeType rootEffectivePeriod,
            boolean latestFromTxServer,
            Optional<String> experimentalBehavior,
            Repository repository)
            throws NotImplementedOperationException, ResourceNotFoundException {
        var resourcesToUpdate = new ArrayList<IDomainResource>();

        // Step 1: Update the Date and the version
        // Need to update the Date element because we're changing the status
        artifactAdapter.setDate(new Date());
        artifactAdapter.setStatus("active");
        artifactAdapter.setVersion(version);
        // Step 2: propagate effectivePeriod if it doesn't exist
        propagageEffectivePeriod(rootEffectivePeriod, artifactAdapter);

        resourcesToUpdate.add(artifactAdapter.get());
        var ownedRelatedArtifacts = artifactAdapter.getOwnedRelatedArtifacts();
        // Step 3 : Get all the OWNED relatedArtifacts
        for (var ownedRelatedArtifact : ownedRelatedArtifacts) {
            var ownedRelatedArtifactReference =
                    KnowledgeArtifactAdapter.getRelatedArtifactReference(ownedRelatedArtifact);
            if (!StringUtils.isBlank(ownedRelatedArtifactReference)) {
                IDomainResource referencedResource;
                // IPrimitiveType<String> ownedResourceReference = ownedRelatedArtifact.getResourceElement();
                Boolean alreadyUpdated = resourcesToUpdate.stream()
                        .map(resource -> AdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum())
                                .createKnowledgeArtifactAdapter(resource))
                        .filter(r -> r.getUrl().equals(Canonicals.getUrl(ownedRelatedArtifactReference)))
                        .findAny()
                        .isPresent();
                if (!alreadyUpdated) {
                    // For composition references, if a version is not specified in the reference then the latest
                    // version
                    // of the referenced artifact should be used. If a version is specified then
                    // `searchRepositoryByCanonicalWithPaging` will
                    // return that version.
                    referencedResource = KnowledgeArtifactAdapter.findLatestVersion(
                                    SearchHelper.searchRepositoryByCanonicalWithPaging(
                                            repository, ownedRelatedArtifactReference))
                            .orElseThrow(() -> new ResourceNotFoundException(String.format(
                                    "Resource with URL '%s' is Owned by this repository and referenced by resource '%s', but was not found on the server.",
                                    ownedRelatedArtifactReference, artifactAdapter.getUrl())));
                    KnowledgeArtifactAdapter searchResultAdapter = AdapterFactory.forFhirVersion(
                                    referencedResource.getStructureFhirVersionEnum())
                            .createKnowledgeArtifactAdapter(referencedResource);

                    checkNonExperimental(referencedResource, experimentalBehavior, repository);
                    resourcesToUpdate.addAll(internalRelease(
                            searchResultAdapter,
                            version,
                            rootEffectivePeriod,
                            latestFromTxServer,
                            experimentalBehavior,
                            repository));
                }
            }
        }

        return resourcesToUpdate;
    }

    private void checkNonExperimental(
            IDomainResource resource, Optional<String> experimentalBehavior, Repository repository)
            throws UnprocessableEntityException {
        if (resource instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.utility.dstu3.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.utility.dstu3.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor.checkNonExperimental(
                    (org.hl7.fhir.dstu3.model.MetadataResource) resource, code, repository, log);
        } else if (resource instanceof org.hl7.fhir.r4.model.MetadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.utility.r4.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.utility.r4.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.checkNonExperimental(
                    (org.hl7.fhir.r4.model.MetadataResource) resource, code, repository, log);
        } else if (resource instanceof org.hl7.fhir.r5.model.MetadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.utility.r5.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.utility.r5.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.checkNonExperimental(
                    (org.hl7.fhir.r5.model.MetadataResource) resource, code, repository, log);
        } else {
            throw new UnprocessableEntityException(resource.getClass().getName() + " not supported");
        }
    }

    private void propagageEffectivePeriod(
            ICompositeType rootEffectivePeriod, KnowledgeArtifactAdapter artifactAdapter) {
        if (rootEffectivePeriod instanceof org.hl7.fhir.dstu3.model.Period) {
            org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor.propagageEffectivePeriod(
                    (org.hl7.fhir.dstu3.model.Period) rootEffectivePeriod, artifactAdapter);
        } else if (rootEffectivePeriod instanceof org.hl7.fhir.r4.model.Period) {
            org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.propagageEffectivePeriod(
                    (org.hl7.fhir.r4.model.Period) rootEffectivePeriod, artifactAdapter);
        } else if (rootEffectivePeriod instanceof org.hl7.fhir.r5.model.Period) {
            org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.propagageEffectivePeriod(
                    (org.hl7.fhir.r5.model.Period) rootEffectivePeriod, artifactAdapter);
        } else {
            throw new UnprocessableEntityException(
                    rootEffectivePeriod.getClass().getName() + " not supported");
        }
    }

    private String tryUpdateReferenceToLatestActiveVersion(
            String inputReference, Repository repository, String sourceArtifactUrl) throws ResourceNotFoundException {
        List<KnowledgeArtifactAdapter> matchingResources = VisitorHelper.getMetadataResourcesFromBundle(
                        SearchHelper.searchRepositoryByCanonicalWithPaging(repository, inputReference))
                .stream()
                .map(r -> AdapterFactory.forFhirVersion(r.getStructureFhirVersionEnum())
                        .createKnowledgeArtifactAdapter((IDomainResource) r))
                .filter(a -> a.getStatus().equals("active"))
                .collect(Collectors.toList());

        if (matchingResources.isEmpty()) {
            return inputReference;
        } else {
            // TODO: Log which version was selected
            matchingResources.sort(
                    comparing(r -> ((KnowledgeArtifactAdapter) r).getVersion()).reversed());
            var latestActiveVersion = matchingResources.get(0);
            String latestActiveReference = latestActiveVersion.hasVersion()
                    ? String.format("%s|%s", latestActiveVersion.getUrl(), latestActiveVersion.getVersion())
                    : latestActiveVersion.getUrl();
            return latestActiveReference;
        }
    }

    private Optional<String> getReleaseVersion(
            String version, Optional<String> versionBehavior, String existingVersion, FhirVersionEnum fhirVersion)
            throws UnprocessableEntityException {
        switch (fhirVersion) {
            case DSTU3:
                return org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor.getReleaseVersion(
                        version, versionBehavior, existingVersion);
            case R4:
                return org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.getReleaseVersion(
                        version, versionBehavior, existingVersion);
            case R5:
                return org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.getReleaseVersion(
                        version, versionBehavior, existingVersion);
            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            default:
                throw new UnprocessableEntityException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    private void updateReleaseLabel(IBaseResource artifact, String releaseLabel) throws IllegalArgumentException {
        if (artifact instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactReleaseVisitor.updateReleaseLabel(
                    (org.hl7.fhir.dstu3.model.MetadataResource) artifact, releaseLabel);
        } else if (artifact instanceof org.hl7.fhir.r4.model.MetadataResource) {
            org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.updateReleaseLabel(
                    (org.hl7.fhir.r4.model.MetadataResource) artifact, releaseLabel);
        } else if (artifact instanceof org.hl7.fhir.r5.model.MetadataResource) {
            org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.updateReleaseLabel(
                    (org.hl7.fhir.r5.model.MetadataResource) artifact, releaseLabel);
        } else {
            throw new UnprocessableEntityException(artifact.getClass().getName() + " not supported");
        }
    }

    private Optional<IDomainResource> checkIfReferenceInList(
            String referenceToCheck, List<IDomainResource> resourceList) {
        Optional<IDomainResource> updatedReference = Optional.ofNullable(null);
        for (var resource : resourceList) {
            String referenceURL = Canonicals.getUrl(referenceToCheck);
            String currentResourceURL = AdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum())
                    .createKnowledgeArtifactAdapter(resource)
                    .getUrl();
            if (referenceURL.equals(currentResourceURL)) {
                return Optional.of(resource);
            }
        }
        return updatedReference;
    }

    private Optional<IDomainResource> checkIfReferenceInList(
            IDependencyInfo artifactToUpdate, List<IDomainResource> resourceList) {
        Optional<IDomainResource> updatedReference = Optional.ofNullable(null);
        for (var resource : resourceList) {
            String referenceURL = Canonicals.getUrl(artifactToUpdate.getReference());
            String currentResourceURL = AdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum())
                    .createKnowledgeArtifactAdapter(resource)
                    .getUrl();
            if (referenceURL.equals(currentResourceURL)) {
                return Optional.of(resource);
            }
        }
        return updatedReference;
    }

    private void checkReleasePreconditions(KnowledgeArtifactAdapter artifact, Date approvalDate)
            throws PreconditionFailedException {
        if (artifact == null) {
            throw new ResourceNotFoundException("Resource not found.");
        }

        if (!artifact.getStatus().equals("draft")) {
            throw new PreconditionFailedException(String.format(
                    "Resource with ID: '%s' does not have a status of 'draft'.",
                    artifact.get().getIdElement().getIdPart()));
        }
        if (approvalDate == null) {
            throw new PreconditionFailedException(String.format(
                    "The artifact must be approved (indicated by approvalDate) before it is eligible for release."));
        }
        if (approvalDate.before(artifact.getDate())) {
            throw new PreconditionFailedException(String.format(
                    "The artifact was approved on '%s', but was last modified on '%s'. An approval must be provided after the most-recent update.",
                    approvalDate, artifact.getDate()));
        }
    }

    private List<IBaseBackboneElement> findArtifactCommentsToUpdate(
            IBaseResource artifact, String releaseVersion, Repository repository) {
        if (artifact instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            return org
                    .opencds
                    .cqf
                    .fhir
                    .utility
                    .visitor
                    .dstu3
                    .KnowledgeArtifactReleaseVisitor
                    .findArtifactCommentsToUpdate(
                            (org.hl7.fhir.dstu3.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r4.model.MetadataResource) {
            return org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.r4.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r5.model.MetadataResource) {
            return org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactReleaseVisitor.findArtifactCommentsToUpdate(
                            (org.hl7.fhir.r5.model.MetadataResource) artifact, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else {
            throw new UnprocessableEntityException("Version not supported");
        }
    }

    private void checkReleaseVersion(String version, Optional<String> versionBehavior)
            throws UnprocessableEntityException {
        if (!versionBehavior.isPresent()) {
            throw new UnprocessableEntityException(
                    "'versionBehavior' must be provided as an argument to the $release operation. Valid values are 'default', 'check', 'force'.");
        }
        checkVersionValidSemver(version);
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
            throw new UnprocessableEntityException(
                    "The version must be in the format MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH.REVISION");
        }
    }

    @Override
    public IBase visit(PlanDefinitionAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(
            KnowledgeArtifactAdapter knowledgeArtifactAdapter,
            Repository repository,
            IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }
}

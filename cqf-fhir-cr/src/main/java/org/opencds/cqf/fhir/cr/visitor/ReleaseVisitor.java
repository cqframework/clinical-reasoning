package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.createAdapterForResource;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReleaseVisitor extends BaseKnowledgeArtifactVisitor {
    private static final String NOT_SUPPORTED = " not supported";
    private static final String ACTIVE = "active";
    private static final String DRAFT = "draft";
    private Logger logger = LoggerFactory.getLogger(ReleaseVisitor.class);
    private static final String DEPENDSON = "depends-on";
    private static final String VALUESET = "ValueSet";
    protected final TerminologyServerClient terminologyServerClient;

    public ReleaseVisitor(Repository repository) {
        super(repository);
        terminologyServerClient = new TerminologyServerClient(fhirContext());
    }

    public ReleaseVisitor(Repository repository, TerminologyServerClient terminologyServerClient) {
        super(repository);
        this.terminologyServerClient = terminologyServerClient;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IBase visit(IKnowledgeArtifactAdapter rootAdapter, IBaseParameters operationParameters) {
        // Setup and parameter validation
        final var rootLibrary = rootAdapter.get();
        final var current = new Date();
        final boolean latestFromTxServer = VisitorHelper.getBooleanParameter("latestFromTxServer", operationParameters)
                .orElse(false);
        final Optional<IEndpointAdapter> terminologyEndpoint = VisitorHelper.getResourceParameter(
                        "terminologyEndpoint", operationParameters)
                .map(r -> (IEndpointAdapter) createAdapterForResource(r));
        if (latestFromTxServer && !terminologyEndpoint.isPresent()) {
            throw new UnprocessableEntityException("latestFromTxServer = true but no terminologyEndpoint is available");
        }
        final var version = VisitorHelper.getStringParameter("version", operationParameters)
                .orElseThrow(() -> new UnprocessableEntityException("Version must be present"));
        final var releaseLabel = VisitorHelper.getStringParameter("releaseLabel", operationParameters)
                .orElse("");
        final var versionBehavior = VisitorHelper.getStringParameter("versionBehavior", operationParameters);
        final var requireNonExperimental = VisitorHelper.getStringParameter(
                        "requireNonExperimental", operationParameters)
                .map(e -> {
                    // if the root artifact is experimental then we don't need to check for experimental children
                    if (rootAdapter.getExperimental()) {
                        return "none";
                    } else {
                        return e;
                    }
                });
        checkReleaseVersion(version, versionBehavior);
        checkReleasePreconditions(rootAdapter, rootAdapter.getApprovalDate());
        updateReleaseLabel(rootLibrary, releaseLabel);
        // Determine which version should be used.
        final var existingVersion =
                rootAdapter.hasVersion() ? rootAdapter.getVersion().replace("-draft", "") : null;
        final var releaseVersion = getReleaseVersion(version, versionBehavior, existingVersion, fhirVersion())
                .orElseThrow(
                        () -> new UnprocessableEntityException("Could not resolve a version for the root artifact."));
        final var rootEffectivePeriod = rootAdapter.getEffectivePeriod();
        var ownedComponentMap = new HashMap<String, Boolean>();
        // true -> canonical key refers to an Owned Component
        ownedComponentMap.put(rootAdapter.getCanonical(), true);

        // get all components and update to latest as appropriate
        var updatedComponents = updateAllComponents(
                rootAdapter, latestFromTxServer, ownedComponentMap, terminologyEndpoint.orElse(null));

        // check experimental status
        updatedComponents.forEach(r -> checkNonExperimental(r, requireNonExperimental, repository));

        // filter for owned components
        var updatedOwnedComponents = updatedComponents.stream()
                .filter(r ->
                        ownedComponentMap.get(((IKnowledgeArtifactAdapter) createAdapterForResource(r)).getCanonical()))
                .toList();

        // update metadata for owned components
        updatedOwnedComponents.forEach(r -> updateMetadata(
                (IKnowledgeArtifactAdapter) createAdapterForResource(r), releaseVersion, rootEffectivePeriod, current));
        var rootArtifactOriginalDependencies = new ArrayList<IDependencyInfo>(rootAdapter.getDependencies());
        // Get list of extensions which need to be preserved
        var originalDependenciesWithExtensions = rootArtifactOriginalDependencies.stream()
                .filter(dep -> dep.getExtension() != null && !dep.getExtension().isEmpty())
                .collect(Collectors.toList());
        // Delete all depends-on RAs in the root artifact
        var noDeps = rootAdapter.getRelatedArtifact();
        noDeps.removeIf(
                ra -> IKnowledgeArtifactAdapter.getRelatedArtifactType(ra).equalsIgnoreCase(DEPENDSON));
        rootAdapter.setRelatedArtifact(noDeps);
        // extract expansion parameters
        var expansionParameters = rootAdapter.getExpansionParameters();
        var systemVersionParams = expansionParameters
                .map(p -> VisitorHelper.getStringListParameter(Constants.SYSTEM_VERSION, p)
                        .orElse(null))
                .orElse(new ArrayList<>());
        var canonicalVersionParams = expansionParameters
                .map(p -> VisitorHelper.getStringListParameter(Constants.CANONICAL_VERSION, p)
                        .orElse(null))
                .orElse(new ArrayList<>());

        // Report all dependencies, resolving unversioned dependencies to the latest known version, recursively
        var gatheredResources = new HashSet<String>();
        gatherDependencies(
                rootAdapter,
                rootAdapter,
                gatheredResources,
                updatedComponents,
                new HashMap<>(),
                systemVersionParams,
                canonicalVersionParams,
                latestFromTxServer,
                terminologyEndpoint.orElse(null));

        if (rootAdapter.get().fhirType().equals("Library")) {
            ((ILibraryAdapter) rootAdapter).setExpansionParameters(systemVersionParams, canonicalVersionParams);
        }

        // remove duplicates and add
        var relatedArtifacts = rootAdapter.getRelatedArtifact();
        var distinctResolvedRelatedArtifacts = new ArrayList<>(relatedArtifacts);
        distinctResolvedRelatedArtifacts.clear();
        for (var resolvedRelatedArtifact : relatedArtifacts) {
            var relatedArtifactReference =
                    IKnowledgeArtifactAdapter.getRelatedArtifactReference(resolvedRelatedArtifact);
            var isDistinct = distinctResolvedRelatedArtifacts.stream().noneMatch(distinctRelatedArtifact -> {
                var referenceNotInArray = relatedArtifactReference.equals(
                        IKnowledgeArtifactAdapter.getRelatedArtifactReference(distinctRelatedArtifact));
                var typeMatches = IKnowledgeArtifactAdapter.getRelatedArtifactType(distinctRelatedArtifact)
                        .equals(IKnowledgeArtifactAdapter.getRelatedArtifactType(resolvedRelatedArtifact));
                return referenceNotInArray && typeMatches;
            });
            if (isDistinct) {
                distinctResolvedRelatedArtifacts.add(resolvedRelatedArtifact);
                // preserve Extensions if found
                originalDependenciesWithExtensions.stream()
                        .filter(originalDep -> Canonicals.getUrl(originalDep.getReference())
                                        .equals(Canonicals.getUrl(relatedArtifactReference))
                                && IKnowledgeArtifactAdapter.getRelatedArtifactType(resolvedRelatedArtifact)
                                        .equalsIgnoreCase(DEPENDSON))
                        .findFirst()
                        .ifPresent(dep -> {
                            ((List<IBaseExtension<?, ?>>) resolvedRelatedArtifact.getExtension())
                                    .addAll(dep.getExtension());
                            originalDependenciesWithExtensions.removeIf(
                                    ra -> ra.getReference().equals(relatedArtifactReference));
                        });
            }
        }

        // Add all updated resources to a transaction bundle for the result
        var transactionBundle = BundleHelper.newBundle(fhirVersion(), null, "transaction");
        for (var artifact : updatedComponents) {
            var entry = PackageHelper.createEntry(artifact, true);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        // update ArtifactComments referencing the old Canonical Reference
        findArtifactCommentsToUpdate(rootLibrary, releaseVersion, repository)
                .forEach(entry -> BundleHelper.addEntry(transactionBundle, entry));
        rootAdapter.setRelatedArtifact(distinctResolvedRelatedArtifacts);

        return repository.transaction(transactionBundle);
    }

    private static void updateMetadata(
            IKnowledgeArtifactAdapter artifactAdapter,
            String version,
            ICompositeType rootEffectivePeriod,
            Date current) {
        artifactAdapter.setDate(current == null ? new Date() : current);
        artifactAdapter.setStatus(ACTIVE);
        artifactAdapter.setVersion(version);
        propagateEffectivePeriod(rootEffectivePeriod, artifactAdapter);
    }

    private List<IDomainResource> updateAllComponents(
            IKnowledgeArtifactAdapter artifactAdapter,
            boolean latestFromTxServer,
            Map<String, Boolean> updatedResourceReferences,
            IEndpointAdapter endpoint)
            throws NotImplementedOperationException, ResourceNotFoundException {
        var updatedComponents = new ArrayList<IDomainResource>();
        // Step 1: add the resource to the list of released resources
        updatedComponents.add(artifactAdapter.get());
        // Step 2 : Go through all the components, update them and recursively release them if Owned
        for (var component : artifactAdapter.getComponents()) {
            final var preReleaseReference = IKnowledgeArtifactAdapter.getRelatedArtifactReference(component);
            final var isOwned = IKnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(component);
            if (!StringUtils.isBlank(preReleaseReference)) {
                var alreadyUpdated = checkIfReferenceInList(preReleaseReference, updatedComponents)
                        .isPresent();
                if (!alreadyUpdated) {
                    // if there is a version specified, it will be used, if not latestComponentRespectingVersions will
                    // query for latest
                    var latest = latestComponentRespectingVersions(
                            preReleaseReference, endpoint, latestFromTxServer, isOwned);
                    if (latest.isPresent()) {
                        updatedResourceReferences.put(latest.get().getCanonical(), isOwned);
                        // release components recursively
                        updatedComponents.addAll(updateAllComponents(
                                latest.get(), latestFromTxServer, updatedResourceReferences, endpoint));
                    }
                }
            }
        }
        return updatedComponents;
    }

    private Optional<IKnowledgeArtifactAdapter> latestComponentRespectingVersions(
            String preReleaseReference, IEndpointAdapter endpoint, boolean latestFromTxServer, boolean isOwned) {
        Optional<IKnowledgeArtifactAdapter> latest = Optional.empty();
        var resourceType = Canonicals.getResourceType(preReleaseReference);
        var prereleaseReferenceVersion = Canonicals.getVersion(preReleaseReference);
        if (isOwned) {
            // get the latest version regardless of status because we own the resource so Drafts are ok (they'll be
            // updated as part of $release)
            latest = VisitorHelper.tryGetLatestVersion(preReleaseReference, repository);
        } else if (resourceType != null
                && resourceType.equals(VALUESET)
                && prereleaseReferenceVersion == null
                && latestFromTxServer) {
            // ValueSets we don't own go to the Tx Server if latestFromTxServer is true
            // we trust in this case that the Endpoint URL matches up with the Authoritative Source in the ValueSet
            // if this assumption is faulty the only consequence is that the VSet doesn't get resolved
            latest = terminologyServerClient
                    .getLatestNonDraftResource(endpoint, preReleaseReference)
                    .map(r -> (IKnowledgeArtifactAdapter) createAdapterForResource(r));
        } else {
            // get the latest ACTIVE version, if not fallback to the latest non-DRAFT version
            latest = VisitorHelper.tryGetLatestVersionWithStatus(preReleaseReference, repository, ACTIVE)
                    .or(() -> VisitorHelper.tryGetLatestVersionExceptStatus(preReleaseReference, repository, DRAFT));
        }
        return latest;
    }

    /* This method first resolves all the components of an artifact, then converts them into dependencies.
     * This entails carefully making sure that owned components get the right versions
     * It then iterates through the dependencies and resolves and updates them to the latest versions.
     */
    @SuppressWarnings("squid:S1872")
    private void gatherDependencies(
            IKnowledgeArtifactAdapter rootAdapter,
            IKnowledgeArtifactAdapter artifactAdapter,
            Set<String> gatheredResources,
            List<IDomainResource> releasedResources,
            Map<String, IDomainResource> alreadyUpdatedDependencies,
            List<String> systemVersionExpansionParameters,
            List<String> canonicalVersionExpansionParameters,
            boolean latestFromTxServer,
            IEndpointAdapter endpoint) {
        if (artifactAdapter == null) {
            return;
        }
        if (!gatheredResources.contains(artifactAdapter.getCanonical())) {
            gatheredResources.add(artifactAdapter.getCanonical());
            for (final var component : artifactAdapter.getComponents()) {
                // Step 1: Update component, add to the cache
                var maybeLatest = updateComponentAndCache(component, releasedResources, alreadyUpdatedDependencies);
                // convert to dependency
                var componentToDependency = IKnowledgeArtifactAdapter.newRelatedArtifact(
                        fhirVersion(),
                        DEPENDSON,
                        maybeLatest
                                .map(r -> r.getCanonical())
                                .orElse(IKnowledgeArtifactAdapter.getRelatedArtifactReference(component)),
                        maybeLatest.map(a -> a.getDescriptor()).orElse(null));
                // add to dependencies
                var updatedRelatedArtifacts = artifactAdapter.getRelatedArtifact();
                updatedRelatedArtifacts.add(componentToDependency);
                artifactAdapter.setRelatedArtifact(updatedRelatedArtifacts);
            }

            var dependencies = artifactAdapter.getDependencies();
            // Step 2: update dependencies recursively
            for (var dependency : dependencies) {
                IKnowledgeArtifactAdapter dependencyAdapter = null;
                var dependencyUrl = Canonicals.getUrl(dependency.getReference());
                if (dependencyUrl == null) {
                    dependencyUrl = dependency.getReference();
                }
                if (!alreadyUpdatedDependencies.containsKey(dependencyUrl)) {
                    var maybeAdapter = tryResolveDependency(
                            dependency,
                            canonicalVersionExpansionParameters,
                            systemVersionExpansionParameters,
                            latestFromTxServer,
                            endpoint);
                    if (maybeAdapter.isPresent()) {
                        dependencyAdapter = maybeAdapter.get();
                        alreadyUpdatedDependencies.put(dependencyAdapter.getUrl(), dependencyAdapter.get());

                        String url = Canonicals.getUrl(dependencyAdapter.getUrl()) + "|" + dependencyAdapter.getVersion();
                        var existingArtifactsForUrl = SearchHelper.searchRepositoryByCanonicalWithPaging(repository, url);
                        if (BundleHelper.getEntry(existingArtifactsForUrl).isEmpty()) {
                            repository.create(dependencyAdapter.get());
                        }
                    } else {
                        alreadyUpdatedDependencies.put(dependencyUrl, null);
                    }
                } else if (alreadyUpdatedDependencies.get(dependencyUrl) != null) {
                    dependencyAdapter = (IKnowledgeArtifactAdapter)
                            createAdapterForResource(alreadyUpdatedDependencies.get(dependencyUrl));
                } else {
                    // the dependency is not resolvable, we already tried
                    continue;
                }
                if (dependencyAdapter != null) {
                    dependency.setReference(dependencyAdapter.getCanonical());
                    gatherDependencies(
                            rootAdapter,
                            dependencyAdapter,
                            gatheredResources,
                            releasedResources,
                            alreadyUpdatedDependencies,
                            systemVersionExpansionParameters,
                            canonicalVersionExpansionParameters,
                            latestFromTxServer,
                            endpoint);
                }
                // only add the dependency to the manifest if it is from a leaf artifact
                if (!artifactAdapter.getUrl().equals(rootAdapter.getUrl())) {
                    var newDep = IKnowledgeArtifactAdapter.newRelatedArtifact(
                            fhirVersion(),
                            DEPENDSON,
                            dependency.getReference(),
                            dependencyAdapter != null ? dependencyAdapter.getDescriptor() : null);
                    var updatedRelatedArtifacts = rootAdapter.getRelatedArtifact();
                    updatedRelatedArtifacts.add(newDep);
                    rootAdapter.setRelatedArtifact(updatedRelatedArtifacts);
                }
            }
        }
    }

    private Optional<IKnowledgeArtifactAdapter> tryResolveDependency(
            IDependencyInfo dependency,
            List<String> canonicalVersionExpansionParameters,
            List<String> systemVersionExpansionParameters,
            boolean latestFromTxServer,
            IEndpointAdapter endpoint) {
        // try to get versions from expansion parameters if they are available
        String resourceType = getResourceType(dependency);
        if (StringUtils.isBlank(Canonicals.getVersion(dependency.getReference()))) {
            // This needs to be updated once we support requireVersionedDependencies
            getExpansionParametersVersion(
                            dependency,
                            resourceType,
                            canonicalVersionExpansionParameters,
                            systemVersionExpansionParameters)
                    .map(Canonicals::getVersion)
                    .ifPresent(version -> dependency.setReference(dependency.getReference() + "|" + version));
        }

        Optional<IKnowledgeArtifactAdapter> maybeAdapter = Optional.empty();
        // if not available in expansion parameters then try to find the latest version and update the
        // dependency
        if (!StringUtils.isBlank(Canonicals.getVersion(dependency.getReference()))) {
            maybeAdapter = Optional.ofNullable(getArtifactByCanonical(dependency.getReference(), repository));
        } else {
            maybeAdapter =
                    tryFindLatestDependency(dependency.getReference(), resourceType, latestFromTxServer, endpoint);
        }
        return maybeAdapter;
    }

    private Optional<IKnowledgeArtifactAdapter> tryFindLatestDependency(
            String reference, String resourceType, boolean latestFromTxServer, IEndpointAdapter endpoint) {
        Optional<IKnowledgeArtifactAdapter> maybeAdapter = Optional.empty();
        // we trust in this case that the Endpoint URL matches up with the Authoritative Source in the ValueSet
        // if this assumption is faulty the only consequence is that the VSet doesn't get resolved
        if (resourceType != null && resourceType.equals(VALUESET) && latestFromTxServer) {
            maybeAdapter = terminologyServerClient
                    .getLatestNonDraftResource(endpoint, reference)
                    .map(r -> (IKnowledgeArtifactAdapter) createAdapterForResource(r));
        } else {
            // get the latest ACTIVE version, if not fallback to the latest non-DRAFT version
            maybeAdapter = VisitorHelper.tryGetLatestVersionWithStatus(reference, repository, ACTIVE)
                    .or(() -> VisitorHelper.tryGetLatestVersionExceptStatus(reference, repository, DRAFT));
        }
        return maybeAdapter;
    }

    private <T extends ICompositeType & IBaseHasExtensions> Optional<IKnowledgeArtifactAdapter> updateComponentAndCache(
            T component,
            List<IDomainResource> releasedResources,
            Map<String, IDomainResource> alreadyUpdatedDependencies) {
        var maybeLatest = getLatestArtifactAndThrowErrorIfOwnedMissing(component, releasedResources);
        if (maybeLatest.isPresent()) {
            updateCacheAndReference(component, maybeLatest.get(), alreadyUpdatedDependencies);
        }
        return maybeLatest;
    }

    private <T extends ICompositeType & IBaseHasExtensions>
            Optional<IKnowledgeArtifactAdapter> getLatestArtifactAndThrowErrorIfOwnedMissing(
                    T component, List<IDomainResource> releasedResources) {
        var reference = IKnowledgeArtifactAdapter.getRelatedArtifactReference(component);
        var resource = Optional.ofNullable(reference)
                .map(r -> checkIfReferenceInList(r, releasedResources).orElse(null));
        if (IKnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(component) && !resource.isPresent()) {
            // should never happen since we check all references as part of `internalRelease`
            throw new InternalErrorException("Owned resource reference not found during release: " + reference);
        }
        return resource;
    }

    private <T extends ICompositeType & IBaseHasExtensions> void updateCacheAndReference(
            T component,
            IKnowledgeArtifactAdapter updatedResource,
            Map<String, IDomainResource> alreadyUpdatedDependencies) {
        // add to cache if resolvable
        if (!alreadyUpdatedDependencies.containsKey(updatedResource.getUrl())) {
            alreadyUpdatedDependencies.put(updatedResource.getUrl(), updatedResource.get());
        }
        // update the reference
        IKnowledgeArtifactAdapter.setRelatedArtifactReference(
                component, updatedResource.getCanonical(), updatedResource.getDescriptor());
    }

    private Optional<String> getExpansionParametersVersion(
            IDependencyInfo dependency,
            String resourceType,
            List<String> canonicalVersionExpansionParameters,
            List<String> systemVersionExpansionParameters) {
        Optional<String> expansionParametersVersion = Optional.empty();
        // assume if we can't figure out the resource type it's a CodeSystem
        if (resourceType == null || resourceType.equals("CodeSystem")) {
            expansionParametersVersion = systemVersionExpansionParameters.stream()
                    .filter(canonical -> !StringUtils.isBlank(Canonicals.getUrl(canonical)))
                    .filter(canonical -> Canonicals.getUrl(canonical).equals(dependency.getReference()))
                    .findAny();
        } else if (resourceType.equals(VALUESET)) {
            expansionParametersVersion = canonicalVersionExpansionParameters.stream()
                    .filter(canonical -> Canonicals.getUrl(canonical).equals(dependency.getReference()))
                    .findAny();
        }
        return expansionParametersVersion;
    }

    private String getResourceType(IDependencyInfo dependency) {
        return Canonicals.getResourceType(dependency.getReference()) == null
                ? null
                : SearchHelper.getResourceType(repository, dependency).getSimpleName();
    }

    private void checkNonExperimental(
            IDomainResource resource, Optional<String> experimentalBehavior, Repository repository)
            throws UnprocessableEntityException {
        if (resource instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.cr.visitor.dstu3.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.cr.visitor.dstu3.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.checkNonExperimental(
                    (org.hl7.fhir.dstu3.model.MetadataResource) resource, code, repository, logger);
        } else if (resource instanceof org.hl7.fhir.r4.model.MetadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.cr.visitor.r4.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.cr.visitor.r4.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.checkNonExperimental(
                    (org.hl7.fhir.r4.model.MetadataResource) resource, code, repository, logger);
        } else if (resource instanceof org.hl7.fhir.r5.model.MetadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.cr.visitor.r5.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.cr.visitor.r5.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.checkNonExperimental(
                    (org.hl7.fhir.r5.model.MetadataResource) resource, code, repository, logger);
        } else {
            throw new UnprocessableEntityException(resource.getClass().getName() + NOT_SUPPORTED);
        }
    }

    private static void propagateEffectivePeriod(
            ICompositeType rootEffectivePeriod, IKnowledgeArtifactAdapter artifactAdapter) {
        if (rootEffectivePeriod instanceof org.hl7.fhir.dstu3.model.Period) {
            org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.propagateEffectivePeriod(
                    (org.hl7.fhir.dstu3.model.Period) rootEffectivePeriod, artifactAdapter);
        } else if (rootEffectivePeriod instanceof org.hl7.fhir.r4.model.Period) {
            org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.propagateEffectivePeriod(
                    (org.hl7.fhir.r4.model.Period) rootEffectivePeriod, artifactAdapter);
        } else if (rootEffectivePeriod instanceof org.hl7.fhir.r5.model.Period) {
            org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.propagateEffectivePeriod(
                    (org.hl7.fhir.r5.model.Period) rootEffectivePeriod, artifactAdapter);
        } else {
            throw new UnprocessableEntityException(
                    rootEffectivePeriod.getClass().getName() + NOT_SUPPORTED);
        }
    }

    private IKnowledgeArtifactAdapter getArtifactByCanonical(String inputReference, Repository repository) {
        List<IKnowledgeArtifactAdapter> matchingResources = VisitorHelper.getMetadataResourcesFromBundle(
                        SearchHelper.searchRepositoryByCanonicalWithPaging(repository, inputReference))
                .stream()
                .map(r -> (IKnowledgeArtifactAdapter) createAdapterForResource(r))
                .collect(Collectors.toList());
        if (matchingResources.isEmpty()) {
            return null;
        } else if (matchingResources.size() == 1) {
            return matchingResources.get(0);
        } else {
            logger.info("Multiple resources found matching {}, used the first one", inputReference);
            return matchingResources.get(0);
        }
    }

    private Optional<String> getReleaseVersion(
            String version, Optional<String> versionBehavior, String existingVersion, FhirVersionEnum fhirVersion)
            throws UnprocessableEntityException {
        switch (fhirVersion) {
            case DSTU3:
                return org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.getReleaseVersion(
                        version, versionBehavior, existingVersion);
            case R4:
                return org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.getReleaseVersion(
                        version, versionBehavior, existingVersion);
            case R5:
                return org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.getReleaseVersion(
                        version, versionBehavior, existingVersion);
            case DSTU2, DSTU2_1, DSTU2_HL7ORG:
            default:
                throw new UnprocessableEntityException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    private void updateReleaseLabel(IBaseResource artifact, String releaseLabel) throws IllegalArgumentException {
        if (artifact instanceof org.hl7.fhir.dstu3.model.MetadataResource) {
            org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.updateReleaseLabel(
                    (org.hl7.fhir.dstu3.model.MetadataResource) artifact, releaseLabel);
        } else if (artifact instanceof org.hl7.fhir.r4.model.MetadataResource) {
            org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.updateReleaseLabel(
                    (org.hl7.fhir.r4.model.MetadataResource) artifact, releaseLabel);
        } else if (artifact instanceof org.hl7.fhir.r5.model.MetadataResource) {
            org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.updateReleaseLabel(
                    (org.hl7.fhir.r5.model.MetadataResource) artifact, releaseLabel);
        } else {
            throw new UnprocessableEntityException(artifact.getClass().getName() + NOT_SUPPORTED);
        }
    }

    private Optional<IKnowledgeArtifactAdapter> checkIfReferenceInList(
            String referenceToCheck, List<IDomainResource> resourceList) {
        for (var resource : resourceList) {
            String referenceURL = Canonicals.getUrl(referenceToCheck);
            String currentResourceURL = IAdapterFactory.forFhirVersion(resource.getStructureFhirVersionEnum())
                    .createKnowledgeArtifactAdapter(resource)
                    .getUrl();
            if (referenceURL.equals(currentResourceURL)) {
                return Optional.of(resource)
                        .map(res -> IAdapterFactory.forFhirVersion(res.getStructureFhirVersionEnum())
                                .createKnowledgeArtifactAdapter(res));
            }
        }
        return Optional.empty();
    }

    private void checkReleasePreconditions(IKnowledgeArtifactAdapter artifact, Date approvalDate)
            throws PreconditionFailedException {
        if (artifact == null) {
            throw new ResourceNotFoundException("Resource not found.");
        }

        if (!artifact.getStatus().equals(DRAFT)) {
            throw new PreconditionFailedException(String.format(
                    "Resource with ID: '%s' does not have a status of 'draft'.",
                    artifact.get().getIdElement().getIdPart()));
        }
        if (approvalDate == null) {
            throw new PreconditionFailedException(
                    "The artifact must be approved (indicated by approvalDate) before it is eligible for release.");
        }
        if (approvalDate.before(artifact.getDate())) {
            throw new PreconditionFailedException(String.format(
                    "The artifact was approved on '%s', but was last modified on '%s'. An approval must be provided after the most-recent update.",
                    approvalDate, artifact.getDate()));
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
        if (version.contains(DRAFT)) {
            throw new UnprocessableEntityException("The version cannot contain 'draft'");
        }
        if (version.contains("/") || version.contains("\\") || version.contains("|")) {
            throw new UnprocessableEntityException("The version contains illegal characters");
        }
        var pattern = Pattern.compile("^(\\d+\\.)(\\d+\\.)(\\*|\\d+)$", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(version);
        boolean matchFound = matcher.find();
        if (!matchFound) {
            throw new UnprocessableEntityException("The version must be in the format MAJOR.MINOR.PATCH");
        }
    }
}

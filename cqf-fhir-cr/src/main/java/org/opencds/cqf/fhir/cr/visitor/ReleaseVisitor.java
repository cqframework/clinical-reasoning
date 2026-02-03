package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.createAdapterForResource;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.opencds.cqf.fhir.cr.common.ExtensionBuilders;
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
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.client.terminology.GenericTerminologyServerClient;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main visitor/driver for the $release operation.
 * <p>
 * This visitor gathers dependencies for the artifact being released via {@link IDependencyInfo}.
 * By contract, {@link IDependencyInfo#getReference()} is expected to represent a FHIR canonical reference
 * (i.e., {@code url} or {@code url|version}) wherever the dependency can be resolved.
 * Non-canonical references coming from adapters are resolved (when possible) and rewritten to canonical form;
 * unresolved dependencies may remain non-canonical but are added as-is.
 */
public class ReleaseVisitor extends BaseKnowledgeArtifactVisitor {
    private static final String NOT_SUPPORTED = " not supported";
    private Logger logger = LoggerFactory.getLogger(ReleaseVisitor.class);
    protected final ITerminologyServerClient terminologyServerClient;
    private IKnowledgeArtifactAdapter artifactBeingReleasedAdapter;

    // Hold on to terminology sever settings.
    // TerminologyProviderRouters don't keep a reference to the settings, those are for individual clients.
    // In the future allow for the ability to have unique settings for each client type.
    private TerminologyServerClientSettings terminologyServerClientSettings;

    public ReleaseVisitor(IRepository repository) {
        super(repository);
        terminologyServerClient = new GenericTerminologyServerClient(
                fhirContext()); // new FederatedTerminologyProviderRouter(fhirContext());
        this.terminologyServerClientSettings = null;
    }

    public ReleaseVisitor(IRepository repository, ITerminologyServerClient terminologyServerClient) {
        super(repository);
        this.terminologyServerClient = terminologyServerClient;
        this.terminologyServerClientSettings = null;
    }

    public ReleaseVisitor(IRepository repository, TerminologyServerClientSettings terminologyServerClientSettings) {
        super(repository);
        this.terminologyServerClient =
                new GenericTerminologyServerClient(fhirContext(), terminologyServerClientSettings);
        this.terminologyServerClientSettings = terminologyServerClientSettings;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IBase visit(IKnowledgeArtifactAdapter rootAdapter, IBaseParameters operationParameters) {
        artifactBeingReleasedAdapter = rootAdapter;

        // Setup and parameter validation
        final var rootLibrary = rootAdapter.get();
        final var current = new Date();
        final boolean latestFromTxServer = VisitorHelper.getBooleanParameter("latestFromTxServer", operationParameters)
                .orElse(false);
        final Optional<IEndpointAdapter> terminologyEndpoint = VisitorHelper.getResourceParameter(
                        "terminologyEndpoint", operationParameters)
                .map(r -> (IEndpointAdapter) createAdapterForResource(r));
        if (latestFromTxServer && terminologyEndpoint.isEmpty()) {
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
        noDeps.removeIf(ra -> IKnowledgeArtifactAdapter.getRelatedArtifactType(ra)
                .equalsIgnoreCase(Constants.RELATEDARTIFACT_TYPE_DEPENDSON));
        rootAdapter.setRelatedArtifact(noDeps);

        // extract expansion parameters
        var inputExpansionParams = rootAdapter.getExpansionParameters().orElse(null);

        // specify authored expansion params as input expansion parameters
        captureInputExpansionParams(inputExpansionParams, rootAdapter);

        // Report all dependencies, resolving unversioned dependencies to the latest known version, recursively
        var gatheredResources = new HashSet<String>();
        gatherDependencies(
                rootAdapter,
                rootAdapter,
                gatheredResources,
                updatedComponents,
                new HashMap<>(),
                inputExpansionParams,
                latestFromTxServer,
                terminologyEndpoint.orElse(null),
                new ArrayList<>());

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
                                        .equalsIgnoreCase(Constants.RELATEDARTIFACT_TYPE_DEPENDSON))
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

        try {
            // update ArtifactComments referencing the old Canonical Reference
            findArtifactCommentsToUpdate(rootLibrary, releaseVersion, repository)
                    .forEach(entry -> BundleHelper.addEntry(transactionBundle, entry));
        } catch (Exception e) {
            logger.error("Error encountered attempting to update ArtifactComments: {}", e.getMessage());
        }

        rootAdapter.setRelatedArtifact(distinctResolvedRelatedArtifacts);

        return repository.transaction(transactionBundle);
    }

    private void captureInputExpansionParams(
            IBaseParameters inputExpansionParams, IKnowledgeArtifactAdapter rootAdapter) {
        if (this.fhirVersion().equals(FhirVersionEnum.DSTU3)) {
            org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.captureInputExpansionParams(
                    inputExpansionParams, rootAdapter);
        } else if (this.fhirVersion().equals(FhirVersionEnum.R4)) {
            org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.captureInputExpansionParams(
                    inputExpansionParams, rootAdapter);
        } else if (this.fhirVersion().equals(FhirVersionEnum.R5)) {
            org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.captureInputExpansionParams(
                    inputExpansionParams, rootAdapter);
        }
    }

    private static void updateMetadata(
            IKnowledgeArtifactAdapter artifactAdapter,
            String version,
            ICompositeType rootEffectivePeriod,
            Date current) {
        artifactAdapter.setDate(current == null ? new Date() : current);
        artifactAdapter.setStatus(Constants.STATUS_ACTIVE);
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
                && resourceType.equals(Constants.RESOURCETYPE_VALUESET)
                && prereleaseReferenceVersion == null
                && latestFromTxServer) {
            // ValueSets we don't own go to the Tx Server if latestFromTxServer is true
            // we trust in this case that the Endpoint URL matches up with the Authoritative Source in the ValueSet
            // if this assumption is faulty the only consequence is that the VSet doesn't get resolved
            latest = terminologyServerClient
                    .getLatestValueSetResource(endpoint, preReleaseReference)
                    .map(r -> (IKnowledgeArtifactAdapter) createAdapterForResource(r));
        } else {
            // get the latest version - removed non-draft status requirement
            latest = VisitorHelper.tryGetLatestVersion(preReleaseReference, repository);
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
            IBaseParameters inputExpansionParameters,
            boolean latestFromTxServer,
            IEndpointAdapter endpoint,
            List<String> parentRoles) {
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
                        Constants.RELATEDARTIFACT_TYPE_DEPENDSON,
                        maybeLatest
                                .map(IKnowledgeArtifactAdapter::getCanonical)
                                .orElse(IKnowledgeArtifactAdapter.getRelatedArtifactReference(component)),
                        maybeLatest
                                .map(IKnowledgeArtifactAdapter::getDescriptor)
                                .orElse(IKnowledgeArtifactAdapter.getRelatedArtifactDisplay(component)));

                var resourceType = maybeLatest
                        .map(r -> Canonicals.getResourceType(r.getCanonical()))
                        .orElse(Canonicals.getResourceType(
                                IKnowledgeArtifactAdapter.getRelatedArtifactReference(component)));

                ensureResourceTypeExtension(resourceType, componentToDependency);

                // add to dependencies
                var updatedRelatedArtifacts = artifactAdapter.getRelatedArtifact();
                updatedRelatedArtifacts.add(componentToDependency);
                artifactAdapter.setRelatedArtifact(updatedRelatedArtifacts);
            }

            var dependencies = artifactAdapter.getDependencies(this.repository);
            // Step 2: update dependencies recursively
            for (var dependency : dependencies) {
                IKnowledgeArtifactAdapter dependencyAdapter = null;
                var dependencyUrl = Canonicals.getUrl(dependency.getReference());
                if (dependencyUrl == null) {
                    dependencyUrl = dependency.getReference();
                }
                if (!alreadyUpdatedDependencies.containsKey(dependencyUrl)) {
                    var maybeAdapter =
                            tryResolveDependency(dependency, inputExpansionParameters, latestFromTxServer, endpoint);
                    if (maybeAdapter.isPresent()) {
                        dependencyAdapter = maybeAdapter.get();
                        alreadyUpdatedDependencies.put(dependencyAdapter.getUrl(), dependencyAdapter.get());
                        var url = Canonicals.getUrl(dependencyAdapter.getUrl());
                        // TODO: previously we were assuming a version exists - likely because we were only considering
                        // non-draft resources. This will likely need work once requireVersionSpecificReferences is
                        // supported...
                        if (dependencyAdapter.hasVersion()) {
                            url += "|" + dependencyAdapter.getVersion();
                        }
                        var existingArtifactsForUrl =
                                SearchHelper.searchRepositoryByCanonicalWithPaging(repository, url);
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

                    // Classify roles for this dependency to determine if it's key
                    List<String> currentDependencyRoles = DependencyRoleClassifier.classifyDependencyRoles(
                            dependency, artifactAdapter, dependencyAdapter, repository);

                    // Propagate key role from parent if needed
                    if (parentRoles.contains("key") && !currentDependencyRoles.contains("key")) {
                        currentDependencyRoles.add(0, "key"); // Add key at the beginning
                    }

                    gatherDependencies(
                            rootAdapter,
                            dependencyAdapter,
                            gatheredResources,
                            releasedResources,
                            alreadyUpdatedDependencies,
                            inputExpansionParameters,
                            latestFromTxServer,
                            endpoint,
                            currentDependencyRoles);
                }
                // only add the dependency to the manifest if it is from a leaf artifact
                if (!artifactAdapter.getUrl().equals(rootAdapter.getUrl())) {
                    // Safety net: warn if resolved dependency is not a canonical URL
                    if (dependencyAdapter != null && Canonicals.getUrl(dependency.getReference()) == null) {
                        logger.warn(
                                "Resolved dependency reference does not appear to be a canonical URL (url or url|version): '{}', artifact URL: '{}'",
                                dependency.getReference(),
                                artifactAdapter.getUrl());
                    }
                    var newDep = IKnowledgeArtifactAdapter.newRelatedArtifact(
                            fhirVersion(),
                            Constants.RELATEDARTIFACT_TYPE_DEPENDSON,
                            dependency.getReference(),
                            dependencyAdapter != null ? dependencyAdapter.getDescriptor() : null);

                    ensureResourceTypeExtension(getResourceType(dependency), newDep);

                    // Add CRMI dependency management extensions
                    addCrmiExtensionsToRelatedArtifact(
                            newDep, dependency, artifactAdapter, dependencyAdapter, repository, parentRoles);

                    var updatedRelatedArtifacts = rootAdapter.getRelatedArtifact();
                    updatedRelatedArtifacts.add(newDep);
                    rootAdapter.setRelatedArtifact(updatedRelatedArtifacts);
                }
            }

            extractMeasureDirectReferenceCodes(rootAdapter, artifactAdapter, endpoint);
        }
    }

    private void ensureResourceTypeExtension(String resourceType, ICompositeType newDep) {
        if (resourceType != null) {
            if (this.fhirVersion().equals(FhirVersionEnum.R4)) {
                ((org.hl7.fhir.r4.model.RelatedArtifact) newDep)
                        .getResourceElement()
                        .addExtension(Constants.CQF_RESOURCETYPE, new org.hl7.fhir.r4.model.CodeType(resourceType));
            } else if (this.fhirVersion().equals(FhirVersionEnum.R5)) {
                ((org.hl7.fhir.r5.model.RelatedArtifact) newDep)
                        .getResourceElement()
                        .addExtension(Constants.CQF_RESOURCETYPE, new org.hl7.fhir.r5.model.CodeType(resourceType));
            }
        }
    }

    private void extractMeasureDirectReferenceCodes(
            IKnowledgeArtifactAdapter rootAdapter,
            IKnowledgeArtifactAdapter artifactAdapter,
            IEndpointAdapter endpoint) {
        if (artifactAdapter instanceof org.opencds.cqf.fhir.utility.adapter.r4.MeasureAdapter measureAdapter) {
            org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.extractDirectReferenceCodes(
                    rootAdapter, measureAdapter.get(), endpoint, terminologyServerClient);
        } else if (artifactAdapter instanceof org.opencds.cqf.fhir.utility.adapter.r5.MeasureAdapter measureAdapter) {
            org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.extractDirectReferenceCodes(
                    rootAdapter, measureAdapter.get());
        }
    }

    private Optional<IKnowledgeArtifactAdapter> tryResolveDependency(
            IDependencyInfo dependency,
            IBaseParameters inputExpansionParameters,
            boolean latestFromTxServer,
            IEndpointAdapter endpoint) {
        // try to get versions from input expansion parameters if they are available
        String resourceType = getResourceType(dependency);
        if (StringUtils.isBlank(Canonicals.getVersion(dependency.getReference()))) {
            // This needs to be updated once we support requireVersionedDependencies
            getExpansionParametersVersion(dependency, resourceType, inputExpansionParameters)
                    .map(Canonicals::getVersion)
                    .ifPresent(version -> dependency.setReference(dependency.getReference() + "|" + version));
        }

        Optional<IKnowledgeArtifactAdapter> maybeAdapter;
        // if not available in input expansion parameters then try to find the latest version and
        // update the dependency and add to runtime expansion params
        if (!StringUtils.isBlank(Canonicals.getVersion(dependency.getReference()))) {
            maybeAdapter = Optional.ofNullable(getArtifactByCanonical(dependency.getReference(), repository));
        } else {
            maybeAdapter =
                    tryFindLatestDependency(dependency.getReference(), resourceType, latestFromTxServer, endpoint);

            // Only add the expansion parameters entry for versionless references
            if (maybeAdapter.isPresent()
                    && artifactBeingReleasedAdapter instanceof ILibraryAdapter libraryAdapter
                    && this.terminologyServerClientSettings != null) {
                libraryAdapter.ensureExpansionParametersEntry(
                        maybeAdapter.get(), this.terminologyServerClientSettings.getCrmiVersion());
            }
        }

        return maybeAdapter;
    }

    private Optional<IKnowledgeArtifactAdapter> tryFindLatestDependency(
            String reference, String resourceType, boolean latestFromTxServer, IEndpointAdapter endpoint) {
        Optional<IKnowledgeArtifactAdapter> maybeAdapter = Optional.empty();
        // we trust in this case that the Endpoint URL matches up with the Authoritative Source in the ValueSet
        // if this assumption is faulty the only consequence is that the VSet doesn't get resolved
        if (resourceType != null && resourceType.equals(Constants.RESOURCETYPE_VALUESET) && latestFromTxServer) {
            maybeAdapter = terminologyServerClient
                    .getLatestValueSetResource(endpoint, reference)
                    .map(r -> (IKnowledgeArtifactAdapter) createAdapterForResource(r));
        } else if (resourceType != null
                && resourceType.equals(Constants.RESOURCETYPE_CODESYSTEM)
                && latestFromTxServer) {
            maybeAdapter = terminologyServerClient
                    .getCodeSystemResource(endpoint, reference)
                    .map(r -> (IKnowledgeArtifactAdapter) createAdapterForResource(r));
        } else {
            if (resourceType == null) {
                return maybeAdapter;
            }
            // TODO: this not bad... do a little better tho
            var hasUrl = new FhirTerser(fhirContext())
                    .fieldExists(
                            "url",
                            fhirContext().getResourceDefinition(resourceType).newInstance());
            if (hasUrl) {
                // get the latest version - removed non-draft status requirement
                maybeAdapter = VisitorHelper.tryGetLatestVersion(reference, repository);
            }
        }
        return maybeAdapter;
    }

    private <T extends ICompositeType & IBaseHasExtensions> Optional<IKnowledgeArtifactAdapter> updateComponentAndCache(
            T component,
            List<IDomainResource> releasedResources,
            Map<String, IDomainResource> alreadyUpdatedDependencies) {
        var maybeLatest = getLatestArtifactAndThrowErrorIfOwnedMissing(component, releasedResources);
        maybeLatest.ifPresent(iKnowledgeArtifactAdapter ->
                updateCacheAndReference(component, iKnowledgeArtifactAdapter, alreadyUpdatedDependencies));
        return maybeLatest;
    }

    private <T extends ICompositeType & IBaseHasExtensions>
            Optional<IKnowledgeArtifactAdapter> getLatestArtifactAndThrowErrorIfOwnedMissing(
                    T component, List<IDomainResource> releasedResources) {
        var reference = IKnowledgeArtifactAdapter.getRelatedArtifactReference(component);
        var resource = Optional.ofNullable(reference).flatMap(r -> checkIfReferenceInList(r, releasedResources));
        if (IKnowledgeArtifactAdapter.checkIfRelatedArtifactIsOwned(component) && resource.isEmpty()) {
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
            IDependencyInfo dependency, String resourceType, IBaseParameters expansionParameters) {

        Optional<String> expansionParametersVersion = Optional.empty();
        if (expansionParameters != null && !expansionParameters.isEmpty()) {
            // assume if we can't figure out the resource type it's a CodeSystem. This may be a
            if (resourceType == null || resourceType.equals(Constants.RESOURCETYPE_CODESYSTEM)) {
                var systemVersionExpansionParameters =
                        VisitorHelper.getStringListParameter(Constants.SYSTEM_VERSION, expansionParameters);

                if (systemVersionExpansionParameters.isPresent()) {
                    expansionParametersVersion = systemVersionExpansionParameters.get().stream()
                            .filter(svp -> Canonicals.getUrl(svp) != null
                                    && Objects.equals(Canonicals.getUrl(svp), dependency.getReference()))
                            .findAny();
                }
            } else if (resourceType.equals(Constants.RESOURCETYPE_VALUESET)) {
                var valueSetVersionExpansionParameters =
                        VisitorHelper.getStringListParameter(Constants.DEFAULT_VALUESET_VERSION, expansionParameters);

                if (valueSetVersionExpansionParameters.isPresent()) {
                    expansionParametersVersion = valueSetVersionExpansionParameters.get().stream()
                            .filter(svp -> Canonicals.getUrl(svp) != null
                                    && Objects.equals(Canonicals.getUrl(svp), dependency.getReference()))
                            .findAny();
                }
            }
        }

        return expansionParametersVersion;
    }

    private String getResourceType(IDependencyInfo dependency) {
        return Canonicals.getResourceType(dependency.getReference()) == null
                ? null
                : SearchHelper.getResourceType(repository, dependency).getSimpleName();
    }

    private void checkNonExperimental(
            IDomainResource resource, Optional<String> experimentalBehavior, IRepository repository)
            throws UnprocessableEntityException {
        if (resource instanceof org.hl7.fhir.dstu3.model.MetadataResource metadataResource2) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.cr.visitor.dstu3.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.cr.visitor.dstu3.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.checkNonExperimental(
                    metadataResource2, code, repository, logger);
        } else if (resource instanceof org.hl7.fhir.r4.model.MetadataResource metadataResource1) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.cr.visitor.r4.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.cr.visitor.r4.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.checkNonExperimental(
                    metadataResource1, code, repository, logger);
        } else if (resource instanceof org.hl7.fhir.r5.model.MetadataResource metadataResource) {
            var code = experimentalBehavior.isPresent()
                    ? org.opencds.cqf.fhir.cr.visitor.r5.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.fromCode(experimentalBehavior.get())
                    : org.opencds.cqf.fhir.cr.visitor.r5.CRMIReleaseExperimentalBehavior
                            .CRMIReleaseExperimentalBehaviorCodes.NULL;
            org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.checkNonExperimental(
                    metadataResource, code, repository, logger);
        } else {
            throw new UnprocessableEntityException(resource.getClass().getName() + NOT_SUPPORTED);
        }
    }

    private static void propagateEffectivePeriod(
            ICompositeType rootEffectivePeriod, IKnowledgeArtifactAdapter artifactAdapter) {
        if (rootEffectivePeriod instanceof org.hl7.fhir.dstu3.model.Period period2) {
            org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.propagateEffectivePeriod(period2, artifactAdapter);
        } else if (rootEffectivePeriod instanceof org.hl7.fhir.r4.model.Period period1) {
            org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.propagateEffectivePeriod(period1, artifactAdapter);
        } else if (rootEffectivePeriod instanceof org.hl7.fhir.r5.model.Period period) {
            org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.propagateEffectivePeriod(period, artifactAdapter);
        } else {
            throw new UnprocessableEntityException(
                    rootEffectivePeriod.getClass().getName() + NOT_SUPPORTED);
        }
    }

    private IKnowledgeArtifactAdapter getArtifactByCanonical(String inputReference, IRepository repository) {
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
                        "Unsupported version of FHIR: %s".formatted(fhirVersion.getFhirVersionString()));
        }
    }

    private void updateReleaseLabel(IBaseResource artifact, String releaseLabel) throws IllegalArgumentException {
        if (artifact instanceof org.hl7.fhir.dstu3.model.MetadataResource resource2) {
            org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.updateReleaseLabel(resource2, releaseLabel);
        } else if (artifact instanceof org.hl7.fhir.r4.model.MetadataResource resource1) {
            org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.updateReleaseLabel(resource1, releaseLabel);
        } else if (artifact instanceof org.hl7.fhir.r5.model.MetadataResource resource) {
            org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.updateReleaseLabel(resource, releaseLabel);
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

        if (!artifact.getStatus().equals(Constants.STATUS_DRAFT)) {
            throw new PreconditionFailedException("Resource with ID: '%s' does not have a status of 'draft'."
                    .formatted(artifact.get().getIdElement().getIdPart()));
        }
        if (artifact.getDate() == null) {
            throw new PreconditionFailedException(
                    "The artifact must have a last modified date (indicated by date) before it is eligible for release.");
        }
        if (approvalDate == null) {
            throw new PreconditionFailedException(
                    "The artifact must be approved (indicated by approvalDate) before it is eligible for release.");
        }
        if (approvalDate.before(artifact.getDate())) {
            throw new PreconditionFailedException(
                    "The artifact was approved on '%s', but was last modified on '%s'. An approval must be provided after the most-recent update."
                            .formatted(approvalDate, artifact.getDate()));
        }
    }

    private void checkReleaseVersion(String version, Optional<String> versionBehavior)
            throws UnprocessableEntityException {
        if (versionBehavior.isEmpty()) {
            throw new UnprocessableEntityException(
                    "'versionBehavior' must be provided as an argument to the $release operation. Valid values are 'default', 'check', 'force'.");
        }
        checkVersionValidSemver(version);
    }

    private void checkVersionValidSemver(String version) throws UnprocessableEntityException {
        if (version == null || version.isEmpty()) {
            throw new UnprocessableEntityException("The version argument is required");
        }
        if (version.contains(Constants.STATUS_DRAFT)) {
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

    /**
     * Adds CRMI dependency management extensions to a RelatedArtifact element.
     * <p>
     * This method adds three types of extensions:
     * <ul>
     *   <li>crmi-dependencyRole: categorizes the dependency (key, default, example, test)</li>
     *   <li>package-source: identifies which package supplied the dependency</li>
     *   <li>crmi-referenceSource: tracks where the dependency was referenced</li>
     * </ul>
     *
     * @param relatedArtifact the RelatedArtifact to add extensions to
     * @param dependency the dependency information
     * @param sourceArtifact the artifact that has this dependency
     * @param dependencyArtifact the artifact being depended on (may be null)
     * @param repository the repository for looking up additional information
     */
    private void addCrmiExtensionsToRelatedArtifact(
            ICompositeType relatedArtifact,
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter sourceArtifact,
            IKnowledgeArtifactAdapter dependencyArtifact,
            IRepository repository,
            List<String> parentRoles) {

        // 1. Classify dependency roles
        List<String> roles = DependencyRoleClassifier.classifyDependencyRoles(
                dependency, sourceArtifact, dependencyArtifact, repository);

        // 2. Propagate "key" role from parent if this is a transitive dependency
        if (parentRoles != null && parentRoles.contains("key") && !roles.contains("key")) {
            roles.add(0, "key"); // Add key role at the beginning
        }

        dependency.setRoles(roles);

        // 3. Resolve package source
        if (dependencyArtifact != null) {
            PackageSourceResolver.resolvePackageSource(dependencyArtifact, repository)
                    .ifPresent(dependency::setReferencePackageId);
        }

        // 4. Preserve original extensions and add new CRMI extensions
        @SuppressWarnings("unchecked")
        List<IBaseExtension<?, ?>> extensionList =
                (List<IBaseExtension<?, ?>>) ((IBaseHasExtensions) relatedArtifact).getExtension();

        // First, copy any existing extensions from the original dependency
        if (dependency.getExtension() != null) {
            extensionList.addAll(dependency.getExtension());
        }

        // Add role extensions (one per role)
        for (String role : dependency.getRoles()) {
            extensionList.add(ExtensionBuilders.buildDependencyRoleExt(fhirVersion(), role));
        }

        // Add package-source extension if available
        if (dependency.getReferencePackageId() != null) {
            extensionList.add(
                    ExtensionBuilders.buildPackageSourceExt(fhirVersion(), dependency.getReferencePackageId()));
        }

        // Add crmi-referenceSource extensions (one per source/path combination)
        for (String path : dependency.getFhirPaths()) {
            if (dependency.getReferenceSource() != null) {
                extensionList.add(ExtensionBuilders.buildReferenceSourceExt(
                        fhirVersion(), dependency.getReferenceSource(), path));
            }
        }
    }
}

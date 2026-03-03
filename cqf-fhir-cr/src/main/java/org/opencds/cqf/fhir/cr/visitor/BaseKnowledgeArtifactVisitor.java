package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.cr.visitor.VisitorHelper.findUnsupportedCapability;
import static org.opencds.cqf.fhir.cr.visitor.VisitorHelper.processCanonicals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.FhirTerser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyProviderRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseKnowledgeArtifactVisitor implements IKnowledgeArtifactVisitor {
    private static final Logger logger = LoggerFactory.getLogger(BaseKnowledgeArtifactVisitor.class);
    String isOwnedUrl = "http://hl7.org/fhir/StructureDefinition/artifact-isOwned";
    protected final IRepository repository;
    protected final Optional<IValueSetExpansionCache> valueSetExpansionCache;

    protected BaseKnowledgeArtifactVisitor(IRepository repository) {
        this.repository = repository;
        this.valueSetExpansionCache = Optional.empty();
    }

    protected BaseKnowledgeArtifactVisitor(IRepository repository, IValueSetExpansionCache valueSetExpansionCache) {
        this.repository = repository;
        this.valueSetExpansionCache = Optional.ofNullable(valueSetExpansionCache);
    }

    protected FhirContext fhirContext() {
        return repository.fhirContext();
    }

    protected FhirVersionEnum fhirVersion() {
        return fhirContext().getVersion().getVersion();
    }

    protected Optional<IValueSetExpansionCache> getExpansionCache() {
        return valueSetExpansionCache;
    }

    protected List<IBaseBackboneElement> findArtifactCommentsToUpdate(
            IBaseResource artifact, String releaseVersion, IRepository repository) {
        if (artifact instanceof org.hl7.fhir.dstu3.model.MetadataResource resource2) {
            return org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitor.findArtifactCommentsToUpdate(
                            resource2, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r4.model.MetadataResource resource1) {
            return org.opencds.cqf.fhir.cr.visitor.r4.ReleaseVisitor.findArtifactCommentsToUpdate(
                            resource1, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else if (artifact instanceof org.hl7.fhir.r5.model.MetadataResource resource) {
            return org.opencds.cqf.fhir.cr.visitor.r5.ReleaseVisitor.findArtifactCommentsToUpdate(
                            resource, releaseVersion, repository)
                    .stream()
                    .map(r -> (IBaseBackboneElement) r)
                    .collect(Collectors.toList());
        } else {
            throw new UnprocessableEntityException("Version not supported");
        }
    }

    protected List<IDomainResource> getComponents(
            IKnowledgeArtifactAdapter adapter, IRepository repository, ArrayList<IDomainResource> resourcesToUpdate) {
        adapter.getOwnedRelatedArtifacts().stream().forEach(c -> {
            final var preReleaseReference = IKnowledgeArtifactAdapter.getRelatedArtifactReference(c);
            Optional<IKnowledgeArtifactAdapter> maybeArtifact =
                    VisitorHelper.tryGetLatestVersion(preReleaseReference, repository);
            if (maybeArtifact.isPresent()) {
                if (resourcesToUpdate.stream().noneMatch(rtu -> rtu.getId()
                        .equals(maybeArtifact.get().getId().toString()))) {
                    resourcesToUpdate.add(maybeArtifact.get().get());
                    getComponents(maybeArtifact.get(), repository, resourcesToUpdate);
                }
            } else {
                throw new ResourceNotFoundException("Unexpected resource not found when getting components");
            }
        });

        return resourcesToUpdate;
    }

    protected void recursiveGather(
            IKnowledgeArtifactAdapter adapter,
            Map<String, IKnowledgeArtifactAdapter> gatheredResources,
            List<String> capability,
            List<String> include,
            ImmutableTriple<List<String>, List<String>, List<String>> versionTuple)
            throws PreconditionFailedException {
        Map<String, String> igDependencyVersions = extractIgDependencyVersions(adapter);
        recursiveGather(
                adapter, gatheredResources, capability, include, versionTuple, null, null, null, igDependencyVersions);
    }

    protected void recursiveGather(
            IKnowledgeArtifactAdapter adapter,
            Map<String, IKnowledgeArtifactAdapter> gatheredResources,
            List<String> capability,
            List<String> include,
            ImmutableTriple<List<String>, List<String>, List<String>> versionTuple,
            IEndpointAdapter terminologyEndpoint,
            ITerminologyProviderRouter router,
            IBaseOperationOutcome[] messagesWrapper)
            throws PreconditionFailedException {
        Map<String, String> igDependencyVersions = extractIgDependencyVersions(adapter);
        recursiveGather(
                adapter,
                gatheredResources,
                capability,
                include,
                versionTuple,
                terminologyEndpoint,
                router,
                messagesWrapper,
                igDependencyVersions);
    }

    protected void recursiveGather(
            IKnowledgeArtifactAdapter adapter,
            Map<String, IKnowledgeArtifactAdapter> gatheredResources,
            List<String> capability,
            List<String> include,
            ImmutableTriple<List<String>, List<String>, List<String>> versionTuple,
            IEndpointAdapter terminologyEndpoint,
            ITerminologyProviderRouter client,
            IBaseOperationOutcome[] messagesWrapper,
            Map<String, String> igDependencyVersions)
            throws PreconditionFailedException {
        if (adapter == null) {
            return;
        }
        if (!gatheredResources.keySet().contains(adapter.getCanonical())) {
            gatheredResources.put(adapter.getCanonical(), adapter);
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, versionTuple);

            adapter.combineComponentsAndDependencies().stream()
                    // sometimes VS dependencies aren't FHIR resources
                    .filter(ra -> StringUtils.isNotBlank(ra.getReference())
                            && StringUtils.isNotBlank(Canonicals.getResourceType(ra.getReference())))
                    .filter(ra -> {
                        try {
                            var resourceDef =
                                    fhirContext().getResourceDefinition(Canonicals.getResourceType(ra.getReference()));
                            return resourceDef != null;
                        } catch (DataFormatException e) {
                            if (e.getMessage().contains("1684")) {
                                return false;
                            } else {
                                throw new DataFormatException(e.getMessage());
                            }
                        }
                    })
                    .map(ra -> {
                        var hasUrl = new FhirTerser(fhirContext())
                                .fieldExists(
                                        "url",
                                        fhirContext()
                                                .getResourceDefinition(Canonicals.getResourceType(ra.getReference()))
                                                .newInstance());
                        if (hasUrl) {
                            // Try to resolve version using package source and IG dependencies
                            String canonical = resolveCanonicalWithIgVersion(ra.getReference(), igDependencyVersions);
                            return Optional.ofNullable(
                                            SearchHelper.searchRepositoryByCanonicalWithPaging(repository, canonical))
                                    .map(bundle -> findResourceMatchingVersion(bundle, canonical, messagesWrapper))
                                    .orElseGet(() -> tryGetValueSetsFromTxServer(ra, client, terminologyEndpoint));
                        }
                        return null;
                    })
                    .filter(r -> r != null)
                    .map(r -> IAdapterFactory.forFhirVersion(fhirVersion()).createKnowledgeArtifactAdapter(r))
                    .forEach(component -> recursiveGather(
                            component,
                            gatheredResources,
                            capability,
                            include,
                            versionTuple,
                            terminologyEndpoint,
                            client,
                            messagesWrapper,
                            igDependencyVersions));
        }
    }

    protected void addBundleEntry(IBaseBundle bundle, boolean isPut, IKnowledgeArtifactAdapter adapter) {
        if (bundle == null) {
            return;
        }
        if (BundleHelper.getEntryResources(bundle).stream()
                .map(e -> IAdapterFactory.forFhirVersion(fhirVersion())
                        .createKnowledgeArtifactAdapter((IDomainResource) e))
                .filter(mr -> mr.getUrl() != null)
                .noneMatch(mr -> mr.getUrl().equals(adapter.getUrl())
                        && (!mr.hasVersion() || mr.getVersion().equals(adapter.getVersion())))) {
            var entry = PackageHelper.createEntry(adapter.get(), isPut);
            BundleHelper.addEntry(bundle, entry);
        }
    }

    protected <T extends ICompositeType & IBaseHasExtensions> void addRelatedArtifact(
            List<T> relatedArtifacts, IKnowledgeArtifactAdapter adapter) {
        if (relatedArtifacts == null) {
            return;
        }
        var reference = adapter.hasVersion()
                ? adapter.getUrl().concat("|%s".formatted(adapter.getVersion()))
                : adapter.getUrl();
        if (relatedArtifacts.stream().noneMatch(ra -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(ra)
                .equals(reference))) {
            relatedArtifacts.add(IKnowledgeArtifactAdapter.newRelatedArtifact(
                    fhirVersion(), "depends-on", reference, adapter.getDescriptor()));
        }
    }

    private IDomainResource tryGetValueSetsFromTxServer(
            IDependencyInfo ra, ITerminologyProviderRouter router, IEndpointAdapter endpoint) {
        if (router != null
                && endpoint != null
                && Canonicals.getResourceType(ra.getReference()).equals("ValueSet")) {
            return router.getValueSetResource(endpoint, ra.getReference()).orElse(null);
        }
        return null;
    }

    /**
     * Finds a resource in the bundle that matches the version specified in the canonical reference.
     * If a version is specified in the canonical, this method will search through all bundle entries
     * to find a resource with the matching URL and version. If no version is specified, it returns
     * the first entry. If a version is specified but no matching version is found, and messagesWrapper is
     * not null, an issue is added to the OperationOutcome and null is returned.
     *
     * @param bundle The bundle containing search results
     * @param canonical The canonical reference (may include version)
     * @param messagesWrapper Optional array wrapper containing OperationOutcome to add issues to when version mismatch occurs
     * @return The matching resource, null if version specified but not found and messagesWrapper is provided,
     *         or the first resource if no version match is found and messagesWrapper is null
     */
    private IDomainResource findResourceMatchingVersion(
            IBaseBundle bundle, String canonical, IBaseOperationOutcome[] messagesWrapper) {
        var requestedVersion = Canonicals.getVersion(canonical);
        var requestedUrl = Canonicals.getUrl(canonical);

        // If no version was requested, attempt to return the "latest"
        if (requestedVersion == null) {
            return IKnowledgeArtifactAdapter.findLatestVersion(bundle)
                    .orElseGet(() -> (IDomainResource) BundleHelper.getEntryResourceFirstRep(bundle));
        }

        // Search through all entries to find one matching the requested version
        var entries = BundleHelper.getEntryResources(bundle);
        for (var resource : entries) {
            if (resource instanceof IDomainResource domainResource) {
                var adapter =
                        IAdapterFactory.forFhirVersion(fhirVersion()).createKnowledgeArtifactAdapter(domainResource);
                if (adapter.getUrl() != null
                        && adapter.getUrl().equals(requestedUrl)
                        && adapter.hasVersion()
                        && adapter.getVersion().equals(requestedVersion)) {
                    return domainResource;
                }
            }
        }

        // No matching version found
        if (messagesWrapper != null) {
            // For $package operation: add error message and return null (don't include resource)
            var errorMessage = String.format(
                    "Requested version '%s' for resource '%s' not found in repository. Resource will not be included in package.",
                    requestedVersion, canonical);
            // Create messages OperationOutcome if it doesn't exist yet
            if (messagesWrapper[0] == null) {
                messagesWrapper[0] = ca.uhn.fhir.util.OperationOutcomeUtil.newInstance(fhirContext());
            }
            ca.uhn.fhir.util.OperationOutcomeUtil.addIssue(
                    fhirContext(), messagesWrapper[0], "error", errorMessage, null, "processing");
            return null;
        } else {
            // For other operations: log warning and return first entry (backward compatibility)
            var firstResource = (IDomainResource) BundleHelper.getEntryResourceFirstRep(bundle);
            if (firstResource != null) {
                var firstAdapter =
                        IAdapterFactory.forFhirVersion(fhirVersion()).createKnowledgeArtifactAdapter(firstResource);
                logger.warn(
                        "Requested version '{}' for resource '{}' not found. Using version '{}' instead.",
                        requestedVersion,
                        requestedUrl,
                        firstAdapter.getVersion());
            }
            return firstResource;
        }
    }

    /**
     * Extracts dependency version mappings from an ImplementationGuide's dependsOn array.
     * Maps packageId to version for use in resolving dependency versions.
     *
     * @param adapter the artifact adapter (may be an ImplementationGuide or reference one)
     * @return Map of packageId -> version from IG dependencies
     */
    protected Map<String, String> extractIgDependencyVersions(IKnowledgeArtifactAdapter adapter) {
        Map<String, String> dependencyVersions = new HashMap<>();

        if (adapter == null) {
            return dependencyVersions;
        }

        try {
            var resource = adapter.get();

            // Check if this is an ImplementationGuide
            if ("ImplementationGuide".equals(resource.fhirType())) {
                var igAdapter = IAdapterFactory.forFhirVersion(fhirVersion())
                        .createImplementationGuide((IDomainResource) resource);
                populateDependencyVersionsFromIg(igAdapter, dependencyVersions);
            }
            // TODO: Check if this artifact references an IG via composed-of relationship
            // and if so, load the IG and extract its dependencies
        } catch (Exception e) {
            logger.debug("Error extracting IG dependency versions", e);
        }

        return dependencyVersions;
    }

    /**
     * Populates dependency versions from an ImplementationGuide adapter.
     * Extracts packageId and version from the IG's dependsOn array.
     *
     * @param igAdapter the ImplementationGuide adapter
     * @param dependencyVersions map to populate with packageId -> version mappings
     */
    private void populateDependencyVersionsFromIg(
            org.opencds.cqf.fhir.utility.adapter.IImplementationGuideAdapter igAdapter,
            Map<String, String> dependencyVersions) {
        try {
            var resource = igAdapter.get();

            // Access dependsOn array based on FHIR version
            if (resource instanceof org.hl7.fhir.r4.model.ImplementationGuide r4Ig) {
                for (var dep : r4Ig.getDependsOn()) {
                    if (dep.hasPackageId() && dep.hasVersion()) {
                        dependencyVersions.put(dep.getPackageId(), dep.getVersion());
                    }
                }
            } else if (resource instanceof org.hl7.fhir.r5.model.ImplementationGuide r5Ig) {
                for (var dep : r5Ig.getDependsOn()) {
                    if (dep.hasPackageId() && dep.hasVersion()) {
                        dependencyVersions.put(dep.getPackageId(), dep.getVersion());
                    }
                }
            } else if (resource instanceof org.hl7.fhir.dstu3.model.ImplementationGuide dstu3Ig) {
                for (var dep : dstu3Ig.getDependency()) {
                    // DSTU3 uses dependency instead of dependsOn, with type and uri
                    if (dep.hasUri()) {
                        // Extract package info from URI if possible
                        // DSTU3 doesn't have explicit packageId field
                        String uri = dep.getUri();
                        // This is a simplified approach - in DSTU3, package info may not be available
                        logger.debug("DSTU3 IG dependency found but packageId extraction not fully supported: {}", uri);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error populating IG dependency versions", e);
        }
    }

    /**
     * Resolves a canonical URL by appending version from IG dependencies if applicable.
     * If the canonical already has a version, returns it unchanged.
     * Otherwise, tries to determine which package the dependency belongs to and
     * appends the version from the IG's dependsOn array.
     *
     * @param canonical the canonical URL (may or may not include version)
     * @param igDependencyVersions map of packageId -> version from IG
     * @return canonical URL with version appended if resolved, otherwise unchanged
     */
    protected String resolveCanonicalWithIgVersion(String canonical, Map<String, String> igDependencyVersions) {
        if (canonical == null || canonical.isEmpty() || igDependencyVersions.isEmpty()) {
            return canonical;
        }

        // If canonical already has a version, don't modify it
        if (Canonicals.getVersion(canonical) != null) {
            return canonical;
        }

        try {
            // Search for the resource to determine its package source
            var bundle = SearchHelper.searchRepositoryByCanonicalWithPaging(repository, canonical);
            if (bundle == null) {
                return canonical;
            }

            var resource = BundleHelper.getEntryResourceFirstRep(bundle);
            if (resource instanceof IDomainResource domainResource) {
                var adapter =
                        IAdapterFactory.forFhirVersion(fhirVersion()).createKnowledgeArtifactAdapter(domainResource);

                // Use PackageSourceResolver to determine which package this dependency belongs to
                Optional<String> packageSource = PackageSourceResolver.resolvePackageSource(adapter, repository);

                if (packageSource.isPresent()) {
                    String packageId = packageSource.get();
                    // Strip version if present in package source (format: packageId#version)
                    if (packageId.contains("#")) {
                        packageId = packageId.substring(0, packageId.indexOf('#'));
                    }

                    // Look up version from IG dependencies
                    String version = igDependencyVersions.get(packageId);
                    if (version != null && !version.isEmpty()) {
                        logger.debug(
                                "Resolved version '{}' for '{}' from IG dependency '{}'",
                                version,
                                canonical,
                                packageId);
                        return canonical + "|" + version;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error resolving canonical with IG version for: {}", canonical, e);
        }

        return canonical;
    }
}

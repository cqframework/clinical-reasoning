package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.repository.IRepository;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.opencds.cqf.fhir.utility.client.terminology.ArtifactEndpointConfiguration;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.NpmRepository;
import org.opencds.cqf.fhir.utility.terminology.CodeSystems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves conformance resources (primarily StructureDefinitions) using a tiered fallback chain:
 * <ol>
 *   <li>Federated repository (real repo + NPM packages) — try first (fastest, resources already loaded)</li>
 *   <li>In-memory NPM package cache — StructureDefinition URL index built lazily from loaded packages</li>
 *   <li>DefaultProfileValidationSupport — core FHIR base types</li>
 * </ol>
 */
public class ConformanceResourceResolver {
    private static final Logger logger = LoggerFactory.getLogger(ConformanceResourceResolver.class);

    private final IRepository repository;
    private final FhirContext fhirContext;
    private final NpmRepository npmRepository;
    private final IRepository federatedRepository;
    private final DefaultProfileValidationSupport coreSupport;
    private Map<String, IBaseResource> packageCache; // lazy, built on first SD miss
    private Map<String, IBaseResource> resourceCache; // lazy, built on first VS/CS miss

    public ConformanceResourceResolver(IRepository repository) {
        this(repository, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Creates a resolver with full resolution chain.
     *
     * @param repository the FHIR repository
     * @param dependsOnPackages list of [packageId, version] pairs from IG dependsOn
     * @param endpointConfigurations artifact endpoint configurations for fallback resolution
     */
    public ConformanceResourceResolver(
            IRepository repository,
            List<String[]> dependsOnPackages,
            List<ArtifactEndpointConfiguration> endpointConfigurations) {
        this.repository = repository;
        this.fhirContext = repository.fhirContext();
        this.coreSupport = new DefaultProfileValidationSupport(fhirContext);
        this.npmRepository = new NpmRepository(fhirContext, dependsOnPackages);
        var hasDependsOnPackages = dependsOnPackages != null && !dependsOnPackages.isEmpty();
        this.federatedRepository =
                hasDependsOnPackages ? new FederatedRepository(repository, npmRepository) : repository;
        if (hasDependsOnPackages) {
            logger.info("Created FederatedRepository with {} dependsOn package(s)", dependsOnPackages.size());
        } else {
            logger.info("No dependsOn packages found, using repository directly");
        }
    }

    /**
     * Returns a repository that federates the real repository with NPM package resources.
     * When dependsOnPackages is empty, this returns the original repository unchanged.
     */
    public IRepository getFederatedRepository() {
        return federatedRepository;
    }

    /**
     * Resolve a StructureDefinition by canonical URL.
     * Resolution order: federated repository → NPM SD cache → core FHIR
     */
    public IBaseResource resolveStructureDefinition(String canonicalUrl) {
        if (canonicalUrl == null || canonicalUrl.isEmpty()) {
            return null;
        }

        // Tier 1: Federated repository
        var result = resolveFromRepository(canonicalUrl);
        if (result != null) {
            return result;
        }

        // Tier 2: NPM package cache (lazy-built SD URL index)
        result = resolveFromPackageCache(canonicalUrl);
        if (result != null) {
            return result;
        }

        // Tier 3: Core FHIR (DefaultProfileValidationSupport)
        return resolveFromCoreSupport(canonicalUrl);
    }

    private IBaseResource resolveFromRepository(String canonicalUrl) {
        try {
            var sdClass = getStructureDefinitionClass();
            var bundle = SearchHelper.searchRepositoryByCanonicalWithPaging(federatedRepository, canonicalUrl);
            if (bundle != null) {
                var entries = org.opencds.cqf.fhir.utility.BundleHelper.getEntry(bundle);
                if (entries != null && !entries.isEmpty()) {
                    var resource = org.opencds.cqf.fhir.utility.BundleHelper.getEntryResource(
                            fhirContext.getVersion().getVersion(), entries.get(0));
                    if (resource != null && sdClass.isInstance(resource)) {
                        return resource;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not resolve from repository: {}", canonicalUrl, e);
        }
        return null;
    }

    private IBaseResource resolveFromPackageCache(String canonicalUrl) {
        if (packageCache == null) {
            packageCache = buildStructureDefinitionCache(npmRepository.getLoadedPackages());
        }
        return packageCache.get(canonicalUrl);
    }

    @SuppressWarnings("unchecked")
    private IBaseResource resolveFromCoreSupport(String canonicalUrl) {
        try {
            var sdClass = getStructureDefinitionClass();
            return coreSupport.fetchResource(sdClass, canonicalUrl);
        } catch (Exception e) {
            logger.debug("Could not resolve from core support: {}", canonicalUrl, e);
        }
        return null;
    }

    private Class<? extends IBaseResource> getStructureDefinitionClass() {
        return fhirContext.getResourceDefinition("StructureDefinition").getImplementingClass();
    }

    private Map<String, IBaseResource> buildStructureDefinitionCache(List<NpmPackage> packages) {
        Map<String, IBaseResource> cache = new HashMap<>();
        if (packages == null || packages.isEmpty()) {
            return cache;
        }

        var parser = fhirContext.newJsonParser();
        for (var npmPackage : packages) {
            try {
                var sdFiles = npmPackage.listResources("StructureDefinition");
                for (var filename : sdFiles) {
                    loadStructureDefinition(npmPackage, filename, parser, cache);
                }
            } catch (Exception e) {
                logger.debug("Error listing StructureDefinitions from package {}", npmPackage.id(), e);
            }
        }
        return cache;
    }

    private void loadStructureDefinition(
            NpmPackage npmPackage,
            String filename,
            ca.uhn.fhir.parser.IParser parser,
            Map<String, IBaseResource> cache) {
        try (InputStream is = npmPackage.load("package", filename)) {
            var resource = parser.parseResource(is);
            var adapter = IAdapterFactory.forFhirVersion(
                            fhirContext.getVersion().getVersion())
                    .createStructureDefinition(resource);
            var url = adapter.getUrl();
            if (url != null && !url.isEmpty()) {
                cache.put(url, resource);
            }
        } catch (Exception e) {
            logger.debug("Error loading StructureDefinition from package {}: {}", npmPackage.id(), filename, e);
        }
    }

    /**
     * Resolve a resource by canonical URL and resource type.
     * Resolution order: federated repository → NPM resource cache → core FHIR
     */
    public IBaseResource resolveResource(String canonicalUrl, String resourceType) {
        if (canonicalUrl == null || canonicalUrl.isEmpty() || resourceType == null) {
            return null;
        }

        // Tier 1: Federated repository (search by canonical)
        try {
            var bundle = SearchHelper.searchRepositoryByCanonicalWithPaging(federatedRepository, canonicalUrl);
            if (bundle != null) {
                var entries = org.opencds.cqf.fhir.utility.BundleHelper.getEntry(bundle);
                if (entries != null && !entries.isEmpty()) {
                    var resource = org.opencds.cqf.fhir.utility.BundleHelper.getEntryResource(
                            fhirContext.getVersion().getVersion(), entries.get(0));
                    if (resource != null && resource.fhirType().equals(resourceType)) {
                        return resource;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not resolve {} from repository: {}", resourceType, canonicalUrl, e);
        }

        // Tier 2: NPM resource cache (lazy-built)
        var cached = resolveFromResourceCache(canonicalUrl, resourceType);
        if (cached != null) {
            return cached;
        }

        // Tier 3: Core FHIR (for base spec resources)
        try {
            var resourceClass = fhirContext.getResourceDefinition(resourceType).getImplementingClass();
            return coreSupport.fetchResource(resourceClass, canonicalUrl);
        } catch (Exception e) {
            logger.debug("Could not resolve {} from core support: {}", resourceType, canonicalUrl, e);
        }
        return null;
    }

    private IBaseResource resolveFromResourceCache(String canonicalUrl, String resourceType) {
        var key = resourceType + "|" + canonicalUrl;
        if (resourceCache == null) {
            resourceCache = buildResourceCache(npmRepository.getLoadedPackages());
        }
        return resourceCache.get(key);
    }

    private Map<String, IBaseResource> buildResourceCache(List<NpmPackage> packages) {
        Map<String, IBaseResource> cache = new HashMap<>();
        if (packages == null || packages.isEmpty()) {
            return cache;
        }
        var parser = fhirContext.newJsonParser();
        for (var npmPackage : packages) {
            for (var resType : List.of("ValueSet", "CodeSystem")) {
                try {
                    var files = npmPackage.listResources(resType);
                    for (var filename : files) {
                        try (InputStream is = npmPackage.load("package", filename)) {
                            var resource = parser.parseResource(is);
                            var url = extractUrl(resource);
                            if (url != null) {
                                cache.putIfAbsent(resType + "|" + url, resource);
                            }
                        } catch (Exception e) {
                            logger.debug("Error loading {} from package {}: {}", resType, npmPackage.id(), filename, e);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Error listing {} from package {}", resType, npmPackage.id(), e);
                }
            }
        }
        logger.debug("Built resource cache with {} entries", cache.size());
        return cache;
    }

    private String extractUrl(IBaseResource resource) {
        try {
            var adapter = IAdapterFactory.forFhirVersion(
                            fhirContext.getVersion().getVersion())
                    .createKnowledgeArtifactAdapter((org.hl7.fhir.instance.model.api.IDomainResource) resource);
            return adapter.getUrl();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a StructureDefinition adapter for the given resource.
     */
    public IStructureDefinitionAdapter createAdapter(IBaseResource resource) {
        return IAdapterFactory.forFhirVersion(fhirContext.getVersion().getVersion())
                .createStructureDefinition(resource);
    }

    /**
     * Looks up the FHIR resource type for a canonical URL.
     * Falls back to the well-known CodeSystem set when the NPM package index has no entry.
     * Returns null if the URL is not found.
     */
    public String getResourceType(String canonicalUrl) {
        var fromNpm = npmRepository.getResourceType(canonicalUrl);
        if (fromNpm != null) {
            return fromNpm;
        }
        if (CodeSystems.isKnownCodeSystem(canonicalUrl)) {
            return "CodeSystem";
        }
        return null;
    }

    /**
     * Looks up the version for a canonical URL from the NPM package index.
     * Returns null if the URL is not found in any loaded package.
     */
    public String getVersion(String canonicalUrl) {
        return npmRepository.getVersion(canonicalUrl);
    }

    /**
     * Looks up the owning package info for a canonical URL from the NPM package index.
     * Returns null if the URL is not found in any loaded package.
     */
    public NpmRepository.PackageInfo getPackageInfo(String canonicalUrl) {
        return npmRepository.getPackageInfo(canonicalUrl);
    }

    /**
     * Gets the FHIR version enum for this resolver's context.
     */
    public FhirVersionEnum getFhirVersion() {
        return fhirContext.getVersion().getVersion();
    }
}

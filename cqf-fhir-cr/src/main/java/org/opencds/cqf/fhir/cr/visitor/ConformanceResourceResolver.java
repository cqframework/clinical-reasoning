package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.repository.IRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.opencds.cqf.fhir.utility.client.terminology.ArtifactEndpointConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves conformance resources (primarily StructureDefinitions) using a tiered fallback chain:
 * <ol>
 *   <li>Repository — try first (fastest, resources already loaded)</li>
 *   <li>In-memory NPM package cache — loaded from dependsOn packages</li>
 *   <li>DefaultProfileValidationSupport — core FHIR base types</li>
 * </ol>
 */
public class ConformanceResourceResolver {
    private static final Logger logger = LoggerFactory.getLogger(ConformanceResourceResolver.class);

    private final IRepository repository;
    private final FhirContext fhirContext;
    private final Map<String, IBaseResource> packageCache;
    private final List<ArtifactEndpointConfiguration> endpointConfigurations;
    private final DefaultProfileValidationSupport coreSupport;

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
        this.endpointConfigurations = endpointConfigurations != null ? endpointConfigurations : Collections.emptyList();
        this.coreSupport = new DefaultProfileValidationSupport(fhirContext);
        this.packageCache = loadPackageCache(dependsOnPackages);
    }

    /**
     * Resolve a StructureDefinition by canonical URL.
     * Resolution order: repository → NPM cache → core FHIR
     */
    public IBaseResource resolveStructureDefinition(String canonicalUrl) {
        if (canonicalUrl == null || canonicalUrl.isEmpty()) {
            return null;
        }

        // Tier 1: Repository
        var result = resolveFromRepository(canonicalUrl);
        if (result != null) {
            return result;
        }

        // Tier 2: NPM package cache
        result = packageCache.get(canonicalUrl);
        if (result != null) {
            return result;
        }

        // Tier 3: Core FHIR (DefaultProfileValidationSupport)
        return resolveFromCoreSupport(canonicalUrl);
    }

    private IBaseResource resolveFromRepository(String canonicalUrl) {
        try {
            var sdClass = getStructureDefinitionClass();
            var bundle = SearchHelper.searchRepositoryByCanonicalWithPaging(repository, canonicalUrl);
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

    private Map<String, IBaseResource> loadPackageCache(List<String[]> dependsOnPackages) {
        if (dependsOnPackages == null || dependsOnPackages.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, IBaseResource> cache = new HashMap<>();
        FilesystemPackageCacheManager packageManager = null;

        try {
            packageManager = new FilesystemPackageCacheManager.Builder()
                    .withSystemCacheFolder()
                    .build();
        } catch (IOException e) {
            logger.debug("Could not initialize NPM package cache manager", e);
            return cache;
        }

        var parser = fhirContext.newJsonParser();

        for (var pkgInfo : dependsOnPackages) {
            if (pkgInfo == null || pkgInfo.length < 2) {
                continue;
            }
            var packageId = pkgInfo[0];
            var version = pkgInfo[1];

            try {
                NpmPackage npmPackage = packageManager.loadPackage(packageId, version);
                if (npmPackage == null) {
                    continue;
                }

                var sdFiles = npmPackage.listResources("StructureDefinition");
                for (var filename : sdFiles) {
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
                        logger.debug("Error loading StructureDefinition from package {}: {}", packageId, filename, e);
                    }
                }
            } catch (Exception e) {
                logger.debug("Error loading NPM package {}#{}", packageId, version, e);
            }
        }

        return cache;
    }

    /**
     * Creates a StructureDefinition adapter for the given resource.
     */
    public IStructureDefinitionAdapter createAdapter(IBaseResource resource) {
        return IAdapterFactory.forFhirVersion(fhirContext.getVersion().getVersion())
                .createStructureDefinition(resource);
    }

    /**
     * Gets the FHIR version enum for this resolver's context.
     */
    public FhirVersionEnum getFhirVersion() {
        return fhirContext.getVersion().getVersion();
    }
}

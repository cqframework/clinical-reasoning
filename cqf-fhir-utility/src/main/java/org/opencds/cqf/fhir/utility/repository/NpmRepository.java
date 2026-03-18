package org.opencds.cqf.fhir.utility.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fully lazy {@link InMemoryFhirRepository} backed by NPM packages.
 *
 * <p>Takes package IDs/versions but loads nothing at construction time.
 * On the first {@link #read} cache miss, it loads the NPM packages from the local
 * filesystem cache (including transitive dependencies) and tries to find the resource.
 * Found resources are cached via {@link #update} for subsequent {@code read()} and
 * {@code search()} calls.
 *
 * <p><b>Overhead:</b> Zero if all resources are in the primary repository.
 * On first cache miss: one-time cost to load NPM packages from local disk cache.
 * Per-resource miss after that: ~1-5ms to parse a single JSON file.
 */
public class NpmRepository extends InMemoryFhirRepository {
    private static final Logger logger = LoggerFactory.getLogger(NpmRepository.class);

    private final List<String[]> dependsOnPackages;
    private List<NpmPackage> loadedPackages; // null until first miss
    private Map<String, String> resourceTypeIndex; // canonical URL → FHIR resource type
    private Map<String, String> versionIndex; // canonical URL → version
    private Map<String, PackageInfo> packageIndex; // canonical URL → owning package info

    /** Package provenance information for a resource. */
    public record PackageInfo(String packageId, String version, String canonical) {}

    public NpmRepository(FhirContext context, List<String[]> dependsOnPackages) {
        super(context);
        this.dependsOnPackages = dependsOnPackages != null ? dependsOnPackages : List.of();
    }

    @Override
    public <T extends IBaseResource, I extends IIdType> T read(
            Class<T> resourceType, I id, Map<String, String> headers) {
        try {
            return super.read(resourceType, id, headers);
        } catch (ResourceNotFoundException e) {
            if (dependsOnPackages.isEmpty()) {
                throw e;
            }

            ensurePackagesLoaded();

            var filename = id.getResourceType() + "-" + id.getIdPart() + ".json";
            logger.debug(
                    "NpmRepository cache miss for {}/{}, searching {} loaded package(s) for {}",
                    id.getResourceType(),
                    id.getIdPart(),
                    loadedPackages.size(),
                    filename);
            for (var pkg : loadedPackages) {
                try (InputStream is = pkg.load("package", filename)) {
                    if (is != null) {
                        var resource = fhirContext().newJsonParser().parseResource(is);
                        resource.setId(id.getResourceType() + "/" + id.getIdPart());
                        update(resource);
                        logger.debug(
                                "Resolved {}/{} from NPM package {}", id.getResourceType(), id.getIdPart(), pkg.id());
                        return resourceType.cast(resource);
                    }
                } catch (ResourceNotFoundException rnf) {
                    throw rnf;
                } catch (Exception ex) {
                    logger.debug("Error loading {} from package {}: {}", filename, pkg.id(), ex.getMessage());
                }
            }
            logger.debug("Resource {}/{} not found in any loaded NPM package", id.getResourceType(), id.getIdPart());
            throw e;
        }
    }

    /**
     * Returns the loaded NPM packages, triggering lazy loading if needed.
     */
    public List<NpmPackage> getLoadedPackages() {
        ensurePackagesLoaded();
        return loadedPackages;
    }

    /**
     * Looks up the FHIR resource type for a canonical URL from the NPM package index.
     * Returns null if the URL is not found in any loaded package.
     */
    public String getResourceType(String canonicalUrl) {
        ensurePackagesLoaded();
        if (canonicalUrl == null || resourceTypeIndex == null) {
            return null;
        }
        return resourceTypeIndex.get(Canonicals.getUrl(canonicalUrl));
    }

    /**
     * Looks up the version for a canonical URL from the NPM package index.
     * Returns null if the URL is not found in any loaded package.
     */
    public String getVersion(String canonicalUrl) {
        ensurePackagesLoaded();
        if (canonicalUrl == null || versionIndex == null) {
            return null;
        }
        return versionIndex.get(Canonicals.getUrl(canonicalUrl));
    }

    /**
     * Looks up the owning package info for a canonical URL from the NPM package index.
     * Returns null if the URL is not found in any loaded package.
     */
    public PackageInfo getPackageInfo(String canonicalUrl) {
        ensurePackagesLoaded();
        if (canonicalUrl == null || packageIndex == null) {
            return null;
        }
        return packageIndex.get(Canonicals.getUrl(canonicalUrl));
    }

    private synchronized void ensurePackagesLoaded() {
        if (loadedPackages != null) {
            return;
        }
        var result = new ArrayList<NpmPackage>();
        var typeIndex = new HashMap<String, String>();
        var verIndex = new HashMap<String, String>();
        var pkgIndex = new HashMap<String, PackageInfo>();
        logger.info("Loading NPM packages from {} seed(s)", dependsOnPackages.size());
        try {
            var mgr = createPackageCacheManager();
            loadPackagesTransitively(mgr, result, typeIndex, verIndex, pkgIndex);
            logger.info("NPM package loading complete: {} package(s) loaded", result.size());
        } catch (Exception e) {
            logger.warn("Could not initialize NPM package cache manager: {}", e.getMessage(), e);
        }
        resourceTypeIndex = typeIndex;
        versionIndex = verIndex;
        packageIndex = pkgIndex;
        loadedPackages = result;
    }

    private FilesystemPackageCacheManager createPackageCacheManager() throws java.io.IOException {
        // Use the user's FHIR package cache (~/.fhir/packages).
        var cacheDir = new File(System.getProperty("user.home"), ".fhir" + File.separator + "packages");
        cacheDir.mkdirs();
        return new FilesystemPackageCacheManager.Builder()
                .withCacheFolder(cacheDir.getAbsolutePath())
                .build();
    }

    private void loadPackagesTransitively(
            FilesystemPackageCacheManager mgr,
            List<NpmPackage> result,
            Map<String, String> typeIndex,
            Map<String, String> verIndex,
            Map<String, PackageInfo> pkgIndex) {
        Set<String> loaded = new HashSet<>();
        Queue<String[]> toLoad = new LinkedList<>(dependsOnPackages);
        while (!toLoad.isEmpty()) {
            var pkgInfo = toLoad.poll();
            if (pkgInfo == null || pkgInfo.length < 2) {
                continue;
            }
            var key = pkgInfo[0] + "#" + pkgInfo[1];
            if (!loaded.add(key)) {
                continue;
            }
            loadSinglePackage(mgr, pkgInfo, key, result, typeIndex, verIndex, pkgIndex, toLoad);
        }
    }

    private void loadSinglePackage(
            FilesystemPackageCacheManager mgr,
            String[] pkgInfo,
            String key,
            List<NpmPackage> result,
            Map<String, String> typeIndex,
            Map<String, String> verIndex,
            Map<String, PackageInfo> pkgIndex,
            Queue<String[]> toLoad) {
        try {
            var npmPkg = mgr.loadPackage(pkgInfo[0], pkgInfo[1]);
            if (npmPkg == null) {
                logger.warn("NPM package not found: {}", key);
                return;
            }
            result.add(npmPkg);
            logger.info("Loaded NPM package: {}", key);

            indexPackageResources(npmPkg, key, typeIndex, verIndex, pkgIndex);

            for (var dep : npmPkg.dependencies()) {
                var parts = dep.split("#", 2);
                if (parts.length == 2) {
                    toLoad.add(new String[] {parts[0], parts[1]});
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load NPM package {}: {}", key, e.getMessage());
        }
    }

    private void indexPackageResources(
            NpmPackage npmPkg,
            String key,
            Map<String, String> typeIndex,
            Map<String, String> verIndex,
            Map<String, PackageInfo> pkgIndex) {
        try {
            var owningPackage = new PackageInfo(npmPkg.id(), npmPkg.version(), npmPkg.canonical());
            for (var info : npmPkg.listIndexedResources()) {
                if (info.getUrl() == null) {
                    continue;
                }
                if (info.getResourceType() != null) {
                    typeIndex.putIfAbsent(info.getUrl(), info.getResourceType());
                }
                // Stub CodeSystems (content: not-present) are placeholders —
                // their version and package provenance are not meaningful.
                var isStub = "CodeSystem".equals(info.getResourceType()) && "not-present".equals(info.getContent());
                if (!isStub) {
                    if (info.getVersion() != null) {
                        verIndex.putIfAbsent(info.getUrl(), info.getVersion());
                    }
                    pkgIndex.putIfAbsent(info.getUrl(), owningPackage);
                }
            }
        } catch (Exception ex) {
            logger.debug("Error indexing resources from package {}: {}", key, ex.getMessage());
        }
    }
}

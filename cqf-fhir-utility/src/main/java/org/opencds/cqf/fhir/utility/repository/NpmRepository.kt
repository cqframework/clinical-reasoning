package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.rest.param.UriParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.google.common.collect.Multimap
import java.io.File
import java.io.IOException
import java.util.*
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.NpmPackage
import org.opencds.cqf.fhir.utility.BundleHelper
import org.opencds.cqf.fhir.utility.Canonicals.getUrl
import org.opencds.cqf.fhir.utility.Ids.newId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A fully lazy [InMemoryFhirRepository] backed by NPM packages.
 *
 * Takes package IDs/versions but loads nothing at construction time. On the first [.read] cache
 * miss, it loads the NPM packages from the local filesystem cache (including transitive
 * dependencies) and tries to find the resource. Found resources are cached via [.update] for
 * subsequent `read()` and `search()` calls.
 *
 * **Overhead:** Zero if all resources are in the primary repository. On first cache miss: one-time
 * cost to load NPM packages from local disk cache. Per-resource miss after that: ~1-5ms to parse a
 * single JSON file.
 */
class NpmRepository(context: FhirContext, dependsOnPackages: MutableList<Array<String?>?>?) :
    InMemoryFhirRepository(context) {
    private val dependsOnPackages: MutableList<Array<String?>?>
    private var loadedPackages: MutableList<NpmPackage>? = null // null until first miss
    private var resourceTypeIndex: MutableMap<String?, String?>? =
        null // canonical URL → FHIR resource type
    private var versionIndex: MutableMap<String?, String?>? = null // canonical URL → version
    private var packageIndex: MutableMap<String?, PackageInfo?>? =
        null // canonical URL → owning package info
    private var idIndex: MutableMap<String?, String?>? =
        null // canonical URL → resource id (for canonical-by-URL search)

    /** Package provenance information for a resource. */
    @JvmRecord
    data class PackageInfo(val packageId: String?, val version: String?, val canonical: String?)

    init {
        this.dependsOnPackages =
            if (dependsOnPackages != null) dependsOnPackages else mutableListOf<Array<String?>?>()
    }

    override fun <T : IBaseResource, I : IIdType> read(
        resourceType: Class<T>,
        id: I?,
        headers: MutableMap<String?, String?>?,
    ): T? {
        try {
            return super.read(resourceType, id, headers)
        } catch (e: ResourceNotFoundException) {
            if (dependsOnPackages.isEmpty()) {
                throw e
            }

            ensurePackagesLoaded()

            val filename = id!!.getResourceType() + "-" + id.getIdPart() + ".json"
            logger.debug(
                "NpmRepository cache miss for {}/{}, searching {} loaded package(s) for {}",
                id.getResourceType(),
                id.getIdPart(),
                loadedPackages!!.size,
                filename,
            )
            for (pkg in loadedPackages!!) {
                try {
                    pkg.load("package", filename).use { `is` ->
                        if (`is` != null) {
                            val resource = fhirContext().newJsonParser().parseResource(`is`)
                            resource.setId(id.getResourceType() + "/" + id.getIdPart())
                            update<IBaseResource?>(resource)
                            logger.debug(
                                "Resolved {}/{} from NPM package {}",
                                id.getResourceType(),
                                id.getIdPart(),
                                pkg.id(),
                            )
                            return resourceType.cast(resource)
                        }
                    }
                } catch (rnf: ResourceNotFoundException) {
                    throw rnf
                } catch (ex: Exception) {
                    logger.debug(
                        "Error loading {} from package {}: {}",
                        filename,
                        pkg.id(),
                        ex.message,
                    )
                }
            }
            logger.debug(
                "Resource {}/{} not found in any loaded NPM package",
                id.getResourceType(),
                id.getIdPart(),
            )
            throw e
        }
    }

    /**
     * Canonical-by-URL search support: when an in-memory search comes up empty and the search
     * includes a `url` parameter, look up the URL in the package index and trigger [.read] to
     * lazy-load the resource. Then re-run the in-memory search.
     *
     * Without this override, callers that resolve canonicals via search (e.g.
     * `SearchHelper.searchRepositoryByCanonicalWithPaging`) silently miss NPM-only resources
     * because lazy loading is only wired into `read()`.
     */
    override fun <B : IBaseBundle, T : IBaseResource> search(
        bundleType: Class<B>?,
        resourceType: Class<T>,
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val result = super.search(bundleType, resourceType, searchParameters, headers)

        if (
            !BundleHelper.getEntry<IBaseBackboneElement>(result!!).isEmpty() ||
                dependsOnPackages.isEmpty()
        ) {
            return result
        }

        val url: String? = extractUrlSearchParam(searchParameters)
        if (url == null) {
            return result
        }

        ensurePackagesLoaded()

        val indexedType = resourceTypeIndex!!.get(url)
        if (indexedType == null || indexedType != resourceType.getSimpleName()) {
            return result
        }

        val indexedId = idIndex!!.get(url)
        if (indexedId == null) {
            return result
        }

        try {
            val id = newId<IIdType>(fhirContext(), indexedType, indexedId)
            read(resourceType, id, headers)
        } catch (e: Exception) {
            logger.debug("NPM lazy-load by URL '{}' failed: {}", url, e.message)
            return result
        }

        return super.search(bundleType, resourceType, searchParameters, headers)
    }

    /** Returns the loaded NPM packages, triggering lazy loading if needed. */
    fun getLoadedPackages(): MutableList<NpmPackage>? {
        ensurePackagesLoaded()
        return loadedPackages
    }

    /**
     * Looks up the FHIR resource type for a canonical URL from the NPM package index. Returns null
     * if the URL is not found in any loaded package.
     */
    fun getResourceType(canonicalUrl: String?): String? {
        ensurePackagesLoaded()
        if (canonicalUrl == null || resourceTypeIndex == null) {
            return null
        }
        return resourceTypeIndex!!.get(getUrl(canonicalUrl))
    }

    /**
     * Looks up the version for a canonical URL from the NPM package index. Returns null if the URL
     * is not found in any loaded package.
     */
    fun getVersion(canonicalUrl: String?): String? {
        ensurePackagesLoaded()
        if (canonicalUrl == null || versionIndex == null) {
            return null
        }
        return versionIndex!!.get(getUrl(canonicalUrl))
    }

    /**
     * Looks up the owning package info for a canonical URL from the NPM package index. Returns null
     * if the URL is not found in any loaded package.
     */
    fun getPackageInfo(canonicalUrl: String?): PackageInfo? {
        ensurePackagesLoaded()
        if (canonicalUrl == null || packageIndex == null) {
            return null
        }
        return packageIndex!!.get(getUrl(canonicalUrl))
    }

    @Synchronized
    private fun ensurePackagesLoaded() {
        if (loadedPackages != null) {
            return
        }
        val result = ArrayList<NpmPackage>()
        val typeIndex = HashMap<String?, String?>()
        val verIndex = HashMap<String?, String?>()
        val pkgIndex = HashMap<String?, PackageInfo?>()
        val idMap = HashMap<String?, String?>()
        logger.info("Loading NPM packages from {} seed(s)", dependsOnPackages.size)
        try {
            val mgr = createPackageCacheManager()
            loadPackagesTransitively(mgr, result, typeIndex, verIndex, pkgIndex, idMap)
            logger.info("NPM package loading complete: {} package(s) loaded", result.size)
        } catch (e: Exception) {
            logger.warn("Could not initialize NPM package cache manager: {}", e.message, e)
        }
        resourceTypeIndex = typeIndex
        versionIndex = verIndex
        packageIndex = pkgIndex
        idIndex = idMap
        loadedPackages = result
    }

    @Throws(IOException::class)
    private fun createPackageCacheManager(): FilesystemPackageCacheManager {
        // Use the user's FHIR package cache (~/.fhir/packages).
        val cacheDir = File(System.getProperty("user.home"), ".fhir" + File.separator + "packages")
        cacheDir.mkdirs()
        return FilesystemPackageCacheManager.Builder()
            .withCacheFolder(cacheDir.getAbsolutePath())
            .build()
    }

    private fun loadPackagesTransitively(
        mgr: FilesystemPackageCacheManager,
        result: MutableList<NpmPackage>,
        typeIndex: MutableMap<String?, String?>,
        verIndex: MutableMap<String?, String?>,
        pkgIndex: MutableMap<String?, PackageInfo?>,
        idMap: MutableMap<String?, String?>,
    ) {
        val loaded: MutableSet<String?> = HashSet<String?>()
        val toLoad: Queue<Array<String?>?> = LinkedList<Array<String?>?>(dependsOnPackages)
        while (!toLoad.isEmpty()) {
            val pkgInfo = toLoad.poll()
            if (pkgInfo == null || pkgInfo.size < 2) {
                continue
            }
            val key = pkgInfo[0] + "#" + pkgInfo[1]
            if (!loaded.add(key)) {
                continue
            }
            loadSinglePackage(
                mgr,
                pkgInfo,
                key,
                result,
                typeIndex,
                verIndex,
                pkgIndex,
                idMap,
                toLoad,
            )
        }
    }

    private fun loadSinglePackage(
        mgr: FilesystemPackageCacheManager,
        pkgInfo: Array<String?>,
        key: String?,
        result: MutableList<NpmPackage>,
        typeIndex: MutableMap<String?, String?>,
        verIndex: MutableMap<String?, String?>,
        pkgIndex: MutableMap<String?, PackageInfo?>,
        idMap: MutableMap<String?, String?>,
        toLoad: Queue<Array<String?>?>,
    ) {
        try {
            val npmPkg = mgr.loadPackage(pkgInfo[0], pkgInfo[1])
            if (npmPkg == null) {
                logger.warn("NPM package not found: {}", key)
                return
            }
            result.add(npmPkg)
            logger.info("Loaded NPM package: {}", key)

            indexPackageResources(npmPkg, key, typeIndex, verIndex, pkgIndex, idMap)

            for (dep in npmPkg.dependencies()) {
                val parts: Array<String?> = dep.split("#".toRegex(), limit = 2).toTypedArray()
                if (parts.size == 2) {
                    toLoad.add(arrayOf<String?>(parts[0], parts[1]))
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to load NPM package {}: {}", key, e.message)
        }
    }

    private fun indexPackageResources(
        npmPkg: NpmPackage,
        key: String?,
        typeIndex: MutableMap<String?, String?>,
        verIndex: MutableMap<String?, String?>,
        pkgIndex: MutableMap<String?, PackageInfo?>,
        idMap: MutableMap<String?, String?>,
    ) {
        try {
            val owningPackage = PackageInfo(npmPkg.id(), npmPkg.version(), npmPkg.canonical())
            for (info in npmPkg.listIndexedResources()) {
                if (info.getUrl() == null) {
                    continue
                }
                if (info.getResourceType() != null) {
                    typeIndex.putIfAbsent(info.getUrl(), info.getResourceType())
                }
                if (info.getId() != null) {
                    idMap.putIfAbsent(info.getUrl(), info.getId())
                }
                // Stub CodeSystems (content: not-present) are placeholders —
                // their version and package provenance are not meaningful.
                val isStub =
                    "CodeSystem" == info.getResourceType() && "not-present" == info.getContent()
                if (!isStub) {
                    if (info.getVersion() != null) {
                        verIndex.putIfAbsent(info.getUrl(), info.getVersion())
                    }
                    pkgIndex.putIfAbsent(info.getUrl(), owningPackage)
                }
            }
        } catch (ex: Exception) {
            logger.debug("Error indexing resources from package {}: {}", key, ex.message)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(NpmRepository::class.java)

        private fun extractUrlSearchParam(
            searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?
        ): String? {
            if (searchParameters == null || !searchParameters.containsKey("url")) {
                return null
            }
            return searchParameters
                .get("url")
                .stream()
                .flatMap<IQueryParameterType?> { obj: MutableList<IQueryParameterType?>? ->
                    obj!!.stream()
                }
                .filter { o: IQueryParameterType? -> UriParam::class.java.isInstance(o) }
                .map<UriParam?> { obj: IQueryParameterType? -> UriParam::class.java.cast(obj) }
                .map<String?> { obj: UriParam? -> obj!!.getValue() }
                .filter { obj: String? -> Objects.nonNull(obj) }
                .findFirst()
                .orElse(null)
        }
    }
}

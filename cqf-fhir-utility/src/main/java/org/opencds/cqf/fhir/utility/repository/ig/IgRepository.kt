package org.opencds.cqf.fhir.utility.repository.ig

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.api.EncodingEnum
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException
import ca.uhn.fhir.util.BundleBuilder
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.collect.Multimap
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.BundleHelper
import org.opencds.cqf.fhir.utility.BundleHelper.newBundle
import org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource
import org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResponse
import org.opencds.cqf.fhir.utility.BundleHelper.newResponseWithLocation
import org.opencds.cqf.fhir.utility.Canonicals
import org.opencds.cqf.fhir.utility.repository.Repositories
import org.opencds.cqf.fhir.utility.repository.ig.EncodingBehavior.PreserveEncoding
import org.opencds.cqf.fhir.utility.repository.operations.IRepositoryOperationProvider
import org.opencds.cqf.fhir.utility.search.Searches.builder

/**
 * Provides access to FHIR resources stored in a directory structure following Implementation Guide
 * (IG) conventions or KALM (Knowledge Artifact Lifecycle Management) conventions. Supports CRUD
 * operations and resource management based on configurable directory and file naming conventions.
 *
 * **Directory Structure Overview (based on conventions):**
 * <pre>
 * Standard IG Layout:
 * /path/to/ig/root/          (CategoryLayout.FLAT)
 * ├── input/
 * │   ├── [resources/]               (CategoryLayout.DIRECTORY_PER_CATEGORY)
 * │   │   ├── Patient-789.json       (FhirTypeLayout.FLAT)
 * │   │   ├── or
 * │   │   ├── [patient/]             (FhirTypeLayout.DIRECTORY_PER_TYPE)
 * │   │   │   ├── Patient-123.json   (FilenameMode.TYPE_AND_ID)
 * │   │   │   ├── or
 * │   │   │   ├── 456.json           (FilenameMode.ID_ONLY)
 * │   │   │   └── ...
 * │   │   └── ...
 * │   └── vocabulary/                (CategoryLayout.DIRECTORY_PER_CATEGORY)
 * │       ├── ValueSet-ABC.json      (FilenameMode.TYPE_AND_ID)
 * |       ├── or
 * │       ├── DEF.json               (FilenameMode.ID_ONLY)
 * │       ├── input/tests/vocabulary/valueset/ (Test-only terminology fallback)
 * │       └── external/              (External Resources - Read-only, Terminology-only)
 * │           └── CodeSystem-external.json
 * └── ...
 *
 * KALM Project Layout:
 * /path/to/kalm/root/        (CategoryLayout.DEFINITIONAL_AND_DATA)
 * ├── src/
 * │   └── fhir/              (Definitional Resources)
 * │       └── ...
 * └── tests/
 * └── data/
 * └── fhir/          (Test Data Resources)
 * ├── shared/        (Non-compartmental data and overrides)
 * │   └── valueset/Example.json
 * ├── [patient/]     (CompartmentMode.PATIENT)
 * │   └── Patient/123/
 * │       └── Observation-456.json
 * └── ...
 * </pre> *
 *
 * **Compartment Support:**
 *
 * The repository supports FHIR compartment contexts using directory conventions. When using a
 * `CompartmentMode` other than `CompartmentMode.NONE`, resources are organized by compartment type
 * and ID (e.g., `Patient/123/`).
 *
 * **Key Features:**
 * * Supports CRUD operations on FHIR resources.
 * * Handles multiple directory layouts: standard IG conventions and KALM project conventions.
 * * Supports compartment-based resource organization for patient-centric data management.
 * * Handles different filename conventions (TYPE_AND_ID vs ID_ONLY).
 * * Annotates resources with metadata like source path and external designation.
 * * Supports invoking FHIR operations through an [IRepositoryOperationProvider].
 * * Utilizes caching for efficient resource access.
 * * Auto-detects project conventions based on directory structure.
 */
open class IgRepository
@JvmOverloads
constructor(
    private val fhirContext: FhirContext,
    private val root: Path,
    private val conventions: IgConventions = IgConventions.Companion.autoDetect(root),
    private var operationProvider: IRepositoryOperationProvider? = null,
) : IRepository {
    private val resourceMatcher = Repositories.getResourceMatcher(this.fhirContext)

    private val resourceCache: Cache<Path?, Optional<IBaseResource>> =
        CacheBuilder.newBuilder().maximumSize(5000).build<Path?, Optional<IBaseResource>>()
    private val pathResolver = ResourcePathResolver(this.root, this.conventions)
    private val compartmentAssigner =
        CompartmentAssigner(
            this.fhirContext,
            this.conventions.compartmentMode!!,
            this.conventions.compartmentIsolation!!,
        )

    /**
     * Creates a new `IgRepository` with specified conventions and encoding behavior.
     *
     * @param fhirContext The FHIR context to use.
     * @param root The root directory of the IG.
     * @param conventions The conventions defining directory and filename structures.
     * @param operationProvider The operation provider for invoking FHIR operations.
     */
    /**
     * Creates a new `IgRepository` with auto-detected conventions and default encoding behavior.
     * The repository configuration is determined based on the directory structure.
     *
     * @param fhirContext The FHIR context to use for parsing and encoding resources.
     * @param root The root directory of the IG.
     * @see IgConventions.autoDetect
     */

    /**
     * Assigns the operation provider used to execute custom FHIR operations against this
     * repository.
     *
     * @param operationProvider the provider responsible for handling operation invocations
     */
    fun setOperationProvider(operationProvider: IRepositoryOperationProvider) {
        this.operationProvider = operationProvider
    }

    /** Clears all cached resources and path indexes maintained by the repository. */
    fun clearCache() {
        this.resourceCache.invalidateAll()
    }

    /**
     * Removes cached entries for the specified file system paths from both the resource cache and
     * the internal path index.
     *
     * @param paths the set of paths to evict from the cache
     */
    fun clearCache(paths: Iterable<Path?>) {
        this.resourceCache.invalidate(paths)
    }

    /**
     * Reads a resource from the given file path.
     *
     * @param path The path to the resource file.
     * @return An `Optional` containing the resource if found; otherwise, empty.
     */
    protected fun readResource(path: Path): Optional<IBaseResource> {
        val file = path.toFile()
        if (!file.exists()) {
            return Optional.empty()
        }

        val encoding = this.pathResolver.encodingForPath(path)

        val content: String
        try {
            content = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        } catch (e: FileNotFoundException) {
            return Optional.empty()
        } catch (e: IOException) {
            throw UnclassifiedServerFailureException(500, "Unable to read resource from path $path")
        }

        try {
            val resource = parserForEncoding(fhirContext, encoding!!)!!.parseResource(content)

            resource.setUserData(SOURCE_PATH_TAG, path)
            CqlContent.loadCqlContent(resource, path.parent)

            return Optional.of(resource)
        } catch (e: DataFormatException) {
            throw ResourceNotFoundException("Found empty or invalid content at path $path")
        }
    }

    protected fun cachedReadResource(path: Path): Optional<IBaseResource> {
        try {
            return this.resourceCache.get(path) { this.readResource(path) }
        } catch (e: Exception) {
            if (e.cause is RuntimeException) {
                throw e.cause as RuntimeException
            }

            throw UnclassifiedServerFailureException(500, "Unable to read resource from path $path")
        }
    }

    /**
     * Writes a resource to the specified file path.
     *
     * @param <T> The type of the FHIR resource.
     * @param resource The resource to write.
     * @param path The file path to write the resource to. </T>
     */
    protected fun <T : IBaseResource> writeResource(resource: T?, path: Path) {
        try {
            if (path.parent != null) {
                path.parent.toFile().mkdirs()
            }

            FileOutputStream(path.toFile()).use { stream ->
                val encoding = this.pathResolver.encodingForPath(path)
                val result: String =
                    parserForEncoding(fhirContext, encoding!!)!!
                        .setPrettyPrint(true)
                        .encodeResourceToString(resource)
                stream.write(result.toByteArray())
                resource!!.setUserData(SOURCE_PATH_TAG, path)
                this.resourceCache.put(path, Optional.of(resource))
            }
        } catch (e: IOException) {
            throw UnclassifiedServerFailureException(500, "Unable to write resource to path $path")
        } catch (e: SecurityException) {
            throw UnclassifiedServerFailureException(500, "Unable to write resource to path $path")
        }
    }

    /**
     * Reads all resources of a given type from the appropriate directories based on repository
     * conventions and compartment context.
     *
     * Directory scanning behavior:
     * * Flat layout: Scans root/category directory for resources
     * * Type-specific directories: Scans type-specific subdirectories
     * * Compartment-aware: Includes compartment-specific paths when applicable
     *
     * File filtering based on filename conventions:
     * * `FilenameMode.ID_ONLY`: Accepts any file with supported extension
     * * `FilenameMode.TYPE_AND_ID`: Accepts files matching pattern "ResourceType-*"
     *
     * @param <T> The type of the FHIR resource.
     * @param resourceClass The class representing the FHIR resource type.
     * @param directories The list of directories to scan for resources.
     * @return Map of resource IDs to resources found in the directories. </T>
     */
    protected fun <T : IBaseResource> readDirectoriesForResource(
        resourceClass: Class<T>,
        directories: MutableList<Path>,
    ): MutableMap<IIdType?, T?> {
        val resources = ConcurrentHashMap<IIdType?, T?>()
        val resourceFileFilter = this.pathResolver.fileMatcher(resourceClass)

        for (dir in directories) {
            if (!Files.exists(dir)) {
                continue
            }

            // Walk the directory and read all files that match the resource type
            // and file extension
            try {
                Files.list(dir).use { pathsStream ->
                    pathsStream
                        .filter(resourceFileFilter)
                        .parallel()
                        .map { path -> this.cachedReadResource(path) }
                        .filter { obj -> obj!!.isPresent }
                        .map { obj -> obj!!.get() }
                        .forEach { r ->
                            if (r!!.fhirType() != resourceClass.simpleName) {
                                return@forEach
                            }
                            val validatedResource = validateResource(resourceClass, r, r.idElement)
                            resources[r.idElement.toUnqualifiedVersionless()] = validatedResource
                        }
                }
            } catch (e: IOException) {
                throw UnclassifiedServerFailureException(
                    500,
                    "Unable to read resources from path: $dir",
                )
            }
        }

        return resources
    }

    override fun fhirContext(): FhirContext {
        return this.fhirContext
    }

    /**
     * Reads a resource from the repository, considering compartment context.
     *
     * File location resolution:
     * * ID_ONLY: "123.json" (in the appropriate directory based on layout and compartment)
     * * TYPE_AND_ID: "Patient-123.json"
     *
     * Compartment context is determined by repository conventions and resource references.
     *
     * Utilizes cache to improve performance.
     *
     * **Example Usage:**
     * <pre>`IIdType resourceId = new IdType("Observation", "obs-123"); Map<String, String> headers = new HashMap<>(); headers.put(FHIR_COMPARTMENT_HEADER, "Patient/patient-456"); Observation observation = repository.read(Observation.class, resourceId, headers); `</pre>
     *
     * @param <T> The type of the FHIR resource.
     * @param <I> The type of the resource identifier.
     * @param resourceType The class representing the FHIR resource type.
     * @param id The identifier of the resource.
     * @param headers Request headers
     * @return The resource if found.
     * @throws ResourceNotFoundException if the resource is not found. </I></T>
     */
    override fun <T : IBaseResource, I : IIdType> read(
        resourceType: Class<T>,
        id: I,
        headers: MutableMap<String?, String?>?,
    ): T? {

        val assignment =
            this.compartmentAssigner.fromHeaders(headers).or {
                this.compartmentAssigner.fromId(resourceType.simpleName, id)
            }
        val paths = this.pathResolver.candidates(resourceType, id.idPart, assignment)
        val resource =
            paths
                .map { path -> this.cachedReadResource(path!!) }
                .filter { obj -> obj!!.isPresent }
                .map { obj -> obj!!.get() }
                .firstOrNull()

        if (resource != null) {
            return validateResource(resourceType, resource, id)
        }

        throw ResourceNotFoundException(id)
    }

    /**
     * Creates a new resource in the repository, considering compartment context for file placement.
     *
     * The resource will be written to the appropriate directory based on repository conventions and
     * any compartment context provided in headers.
     *
     * **Example Usage:**
     * <pre>`Observation newObservation = new Observation(); newObservation.setId("obs-789"); newObservation.setSubject(new Reference("Patient/patient-123")); MethodOutcome outcome = repository.create(newObservation, null); `</pre>
     *
     * @param <T> The type of the FHIR resource.
     * @param resource The resource to create.
     * @param headers Request headers, may include compartment context via `X-FHIR-Compartment`.
     * @return A [MethodOutcome] containing the outcome of the create operation. </T>
     */
    override fun <T : IBaseResource> create(
        resource: T,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome {
        requireNotNull(resource.idElement.idPart) { "resource id cannot be null" }

        val assignment =
            this.compartmentAssigner.fromHeaders(headers).or {
                this.compartmentAssigner.fromResource(resource)
            }

        val path =
            this.pathResolver.preferredPath(
                resource.javaClass,
                resource.idElement.idPart,
                assignment,
            )
        writeResource(resource, path)

        return MethodOutcome(resource.idElement, true)
    }

    private fun <T : IBaseResource> validateResource(
        resourceType: Class<T>,
        resource: IBaseResource,
        id: IIdType,
    ): T? {
        // All freshly read resources are tagged with their source path
        val path = resource.getUserData(SOURCE_PATH_TAG) as Path?

        if (resourceType.simpleName != resource.fhirType()) {
            throw ResourceNotFoundException(
                "Expected to find a resource with type: ${resourceType.simpleName} at path: $path. Found resource with type ${resource.fhirType()} instead."
            )
        }

        if (!resource.idElement.hasIdPart()) {
            throw ResourceNotFoundException(
                "Expected to find a resource with id: ${id.toUnqualifiedVersionless()} at path: $path. Found resource without an id instead."
            )
        }

        if (id.idPart != resource.idElement.idPart) {
            throw ResourceNotFoundException(
                "Expected to find a resource with id: ${id.idPart} at path: $path. Found resource with an id ${resource.idElement.idPart} instead."
            )
        }

        if (id.hasVersionIdPart() && id.versionIdPart != resource.idElement.versionIdPart) {
            throw ResourceNotFoundException(
                "Expected to find a resource with version: ${id.versionIdPart} at path: $path. Found resource with version ${ resource.idElement.versionIdPart} instead."
            )
        }

        return resourceType.cast(resource)
    }

    /**
     * Updates an existing resource in the repository.
     *
     * **Example Usage:**
     * <pre>`Map<String, String> headers = new HashMap<>(); Patient existingPatient = repository.read(Patient.class, new IdType("Patient", "12345"), headers); existingPatient.addAddress().setCity("New City"); MethodOutcome updateOutcome = repository.update(existingPatient, headers); `</pre>
     *
     * @param <T> The type of the FHIR resource.
     * @param resource The resource to update.
     * @param headers Additional headers (not used in this implementation).
     * @return A [MethodOutcome] containing the outcome of the update operation. </T>
     */
    override fun <T : IBaseResource> update(
        resource: T,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome {
        requireNotNull(resource.idElement.idPart) { "resource id cannot be null" }

        val existingPath = resource.getUserData(SOURCE_PATH_TAG) as Path?

        if (existingPath != null && this.pathResolver.isExternalPath(existingPath)) {
            throw ForbiddenOperationException(
                "Unable to create or update: ${resource.idElement.toUnqualifiedVersionless()}. Resource is marked as external, and external resources are read-only."
            )
        }

        val assignment =
            this.compartmentAssigner.fromHeaders(headers).or {
                this.compartmentAssigner.fromResource(resource)
            }

        val resourceType = resource.javaClass
        val idPart = resource.idElement.idPart

        var targetPath = this.pathResolver.preferredPath(resourceType, idPart, assignment)
        if (
            existingPath != null &&
                (this.conventions.encodingBehavior!!.preserveEncoding ==
                    PreserveEncoding.PRESERVE_ORIGINAL_ENCODING)
        ) {
            val existingEncoding = this.pathResolver.encodingForPath(existingPath)
            if (existingEncoding != null) {
                val targetFilename =
                    this.pathResolver.filenameForEncoding(resourceType, idPart, existingEncoding)
                targetPath =
                    this.pathResolver
                        .preferredDirectory(resourceType, assignment)
                        .resolve(targetFilename)
            }
        }

        if (existingPath != null && existingPath != targetPath) {
            try {
                this.resourceCache.invalidate(existingPath)
                Files.deleteIfExists(existingPath)
            } catch (e: IOException) {
                throw UnclassifiedServerFailureException(
                    500,
                    "Couldn't relocate resource from $existingPath to $targetPath",
                )
            }
        }

        writeResource(resource, targetPath)

        return MethodOutcome(resource.idElement, false)
    }

    /**
     * Deletes a resource from the repository.
     *
     * **Example Usage:**
     * <pre>`IIdType deleteId = new IdType("Patient", "67890"); Map<String, String> headers = new HashMap<>(); MethodOutcome deleteOutcome = repository.delete(Patient.class, deleteId, headers); `</pre>
     *
     * @param <T> The type of the FHIR resource.
     * @param <I> The type of the resource identifier.
     * @param resourceType The class representing the FHIR resource type.
     * @param id The identifier of the resource to delete.
     * @param headers Additional headers (not used in this implementation).
     * @return A [MethodOutcome] containing the outcome of the delete operation. </I></T>
     */
    override fun <T : IBaseResource, I : IIdType> delete(
        resourceType: Class<T>,
        id: I,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome {

        val assignment =
            this.compartmentAssigner.fromHeaders(headers).or {
                this.compartmentAssigner.fromId(resourceType.simpleName, id)
            }

        val paths = this.pathResolver.candidates(resourceType, id!!.idPart, assignment)
        var deleted = false
        for (path in paths) {
            try {
                deleted = Files.deleteIfExists(path)
                if (deleted) {
                    this.resourceCache.invalidate(path)
                    break
                }
            } catch (e: IOException) {
                throw UnclassifiedServerFailureException(500, "Couldn't delete $path")
            }
        }

        if (!deleted) {
            throw ResourceNotFoundException(id)
        }

        return MethodOutcome(id)
    }

    /**
     * Searches for resources matching the given search parameters.
     *
     * **Example Usage:**
     * <pre>`Map<String, List<IQueryParameterType>> searchParameters = new HashMap<>(); searchParameters.put("family", Arrays.asList(new StringParam("Doe"))); Map<String, String> headers = new HashMap<>(); IBaseBundle bundle = repository.search(Bundle.class, Patient.class, searchParameters, headers); `</pre>
     *
     * @param <B> The type of the bundle to return.
     * @param <T> The type of the FHIR resource.
     * @param bundleType The class representing the bundle type.
     * @param resourceType The class representing the FHIR resource type.
     * @param searchParameters The search parameters.
     * @param headers Additional headers (not used in this implementation).
     * @return A bundle containing the matching resources. </T></B>
     */
    override fun <B : IBaseBundle, T : IBaseResource> search(
        bundleType: Class<B>,
        resourceType: Class<T>,
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): B? {

        val assignment =
            this.compartmentAssigner.fromHeaders(headers).or {
                this.compartmentAssigner.fromSearchParameters(
                    resourceType.simpleName,
                    searchParameters,
                )
            }

        val directories = this.pathResolver.directories(resourceType, assignment!!)
        val resourceIdMap = this.readDirectoriesForResource(resourceType, directories)

        val builder = BundleBuilder(this.fhirContext)
        builder.setType("searchset")
        if (searchParameters == null || searchParameters.isEmpty) {
            resourceIdMap.values.forEach { theResource -> builder.addCollectionEntry(theResource) }
            return builder.bundle as B?
        }

        for (resource in resourceIdMap.values) {
            if (allParametersMatch(searchParameters, resource!!)) {
                builder.addCollectionEntry(resource)
            }
        }

        return builder.bundle as B?
    }

    private fun allParametersMatch(
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>,
        resource: IBaseResource,
    ): Boolean {
        for (nextEntry in searchParameters.entries()) {
            val paramName = nextEntry.key
            if (!resourceMatcher.matches(paramName!!, nextEntry.value, resource)) {
                return false
            }
        }

        return true
    }

    /**
     * Invokes a FHIR operation on a resource type.
     *
     * @param <R> The type of the resource returned by the operation.
     * @param <P> The type of the parameters for the operation.
     * @param <T> The type of the resource on which the operation is invoked.
     * @param resourceType The class representing the FHIR resource type.
     * @param name The name of the operation.
     * @param parameters The operation parameters.
     * @param returnType The expected return type.
     * @param headers Additional headers (not used in this implementation).
     * @return The result of the operation. </T></P></R>
     */
    override fun <R : IBaseResource, P : IBaseParameters, T : IBaseResource> invoke(
        resourceType: Class<T>,
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        return invokeOperation(null, resourceType.simpleName, name, parameters)
    }

    /**
     * Invokes a FHIR operation on a specific resource instance.
     *
     * @param <R> The type of the resource returned by the operation.
     * @param <P> The type of the parameters for the operation.
     * @param <I> The type of the resource identifier.
     * @param id The identifier of the resource.
     * @param name The name of the operation.
     * @param parameters The operation parameters.
     * @param returnType The expected return type.
     * @param headers Additional headers (not used in this implementation).
     * @return The result of the operation. </I></P></R>
     */
    override fun <R : IBaseResource, P : IBaseParameters, I : IIdType> invoke(
        id: I?,
        name: String?,
        parameters: P?,
        returnType: Class<R?>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        return invokeOperation(id, id!!.resourceType, name, parameters)
    }

    /**
     * Processes a transaction bundle, supporting PUT, POST, and DELETE entries. Resources are
     * written to the filesystem via the standard [.create], [.update], and [.delete] methods.
     *
     * @param <B> the bundle type
     * @param transaction the transaction bundle to process
     * @param headers request headers
     * @return a response bundle with entry responses for each processed entry
     * @throws NotImplementedOperationException if the transaction contains unsupported entry types
     *   </B>
     */
    override fun <B : IBaseBundle> transaction(
        transaction: B?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val version = transaction!!.structureFhirVersionEnum
        val returnBundle = newBundle(version, "transaction-response") as B?
        BundleHelper.getEntry<IBaseBackboneElement>(transaction)
            .forEach(
                Consumer { e: IBaseBackboneElement? ->
                    if (BundleHelper.isEntryRequestPut(version, e!!)) {
                        val resource = BundleHelper.getEntryResource(version, e)
                        this.update(resource!!, headers)
                        val location = resource.idElement.value
                        BundleHelper.addEntry(
                            returnBundle!!,
                            newEntryWithResponse(
                                version,
                                newResponseWithLocation(version, location),
                            ),
                        )
                    } else if (BundleHelper.isEntryRequestPost(version, e)) {
                        val resource = BundleHelper.getEntryResource(version, e)
                        if (!resource!!.idElement.hasIdPart()) {
                            resource.setId(UUID.randomUUID().toString())
                        }
                        this.create(resource, headers)
                        val location = resource.idElement.value
                        BundleHelper.addEntry(
                            returnBundle!!,
                            newEntryWithResponse(
                                version,
                                newResponseWithLocation(version, location),
                            ),
                        )
                    } else if (BundleHelper.isEntryRequestDelete(version, e)) {
                        val requestId = BundleHelper.getEntryRequestId(version, e)
                        if (requestId == null) {
                            throw ResourceNotFoundException("Trying to delete an entry without id")
                        }
                        val requestUrl = BundleHelper.getEntryRequestUrl(version, e)
                        val resourceType = Canonicals.getResourceType(requestUrl!!)
                        val resourceClass =
                            fhirContext.getResourceDefinition(resourceType).implementingClass
                        this.delete(
                            resourceClass,
                            requestId.withResourceType(resourceType),
                            headers,
                        )
                        BundleHelper.addEntry(
                            returnBundle!!,
                            newEntryWithResponse(
                                version,
                                newResponseWithLocation(version, requestUrl),
                            ),
                        )
                    } else if (
                        BundleHelper.isEntryRequestGet(version, e) &&
                            BundleHelper.getEntryRequestUrl(version, e)!!.contains("?")
                    ) {
                        val searchResult =
                            executeSearchUrl(BundleHelper.getEntryRequestUrl(version, e)!!, headers)
                        BundleHelper.addEntry(returnBundle!!, newEntryWithResource(searchResult))
                    } else {
                        throw NotImplementedOperationException(
                            "Transaction only supports PUT, POST, DELETE, or search-style GET"
                        )
                    }
                }
            )
        return returnBundle
    }

    protected fun <R : IBaseResource> invokeOperation(
        id: IIdType?,
        resourceType: String?,
        operationName: String?,
        parameters: IBaseParameters?,
    ): R? {
        requireNotNull(operationProvider) {
            "No operation provider found. Unable to invoke operations."
        }
        return operationProvider!!.invokeOperation<IPrimitiveType<String?>?, R?>(
            this,
            id,
            resourceType,
            operationName,
            parameters,
        )
    }

    private fun executeSearchUrl(
        requestUrl: String,
        headers: MutableMap<String?, String?>?,
    ): IBaseBundle {
        val queryIdx = requestUrl.indexOf('?')
        val resourceType = if (queryIdx < 0) requestUrl else requestUrl.substring(0, queryIdx)
        val queryString = if (queryIdx < 0) "" else requestUrl.substring(queryIdx + 1)
        val resourceClass =
            fhirContext.getResourceDefinition(resourceType).implementingClass
                as Class<IBaseResource>
        val bundleClass =
            fhirContext.getResourceDefinition("Bundle").implementingClass as Class<IBaseBundle>
        return this.search(
            bundleClass,
            resourceClass,
            IgRepository.Companion.parseSearchQuery(queryString),
            headers,
        )!!
    }

    companion object {
        // Metadata fields attached to resources that are read from the repository
        // This fields are used to determine if a resource is external, and to
        // maintain the original encoding of the resource.
        const val SOURCE_PATH_TAG: String = "sourcePath" // Path
        const val FHIR_COMPARTMENT_HEADER: String = "X-FHIR-Compartment"

        private fun parserForEncoding(
            fhirContext: FhirContext,
            encodingEnum: EncodingEnum,
        ): IParser? {
            return when (encodingEnum) {
                EncodingEnum.JSON -> fhirContext.newJsonParser()
                EncodingEnum.XML -> fhirContext.newXmlParser()
                EncodingEnum.RDF -> fhirContext.newRDFParser()
                EncodingEnum.NDJSON -> fhirContext.newNDJsonParser()
            }
        }

        private fun parseSearchQuery(
            queryString: String
        ): Multimap<String?, MutableList<IQueryParameterType?>?>? {
            val builder = builder()
            if (queryString.isEmpty()) {
                return builder.build()
            }
            for (pair in
                queryString.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val eq = pair.indexOf('=')
                val key = if (eq < 0) pair else pair.substring(0, eq)
                val value =
                    if (eq < 0) ""
                    else URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8)
                when (key) {
                    "url" -> builder.withUriParam("url", value)
                    "version" -> builder.withTokenParam("version", value)
                    else -> builder.withStringParam(key, value)
                }
            }
            return builder.build()
        }
    }
}

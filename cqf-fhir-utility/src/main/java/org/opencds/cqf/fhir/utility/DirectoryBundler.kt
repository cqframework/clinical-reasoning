package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.api.BundleInclusionRule
import ca.uhn.fhir.model.valueset.BundleTypeEnum
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.api.BundleLinks
import ca.uhn.fhir.util.BundleUtil
import java.io.File
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.FileVisitOption
import java.nio.file.Files
import org.apache.commons.io.FileUtils
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** This class takes a directory and bundles all FHIR resources found in it recursively. */
class DirectoryBundler(private val fhirContext: FhirContext) {
    private var xml: IParser? = null
    private var json: IParser? = null

    /**
     * Recursively searches all files and sub-directory and parses all xml and json FHIR resources.
     * Adds all resources to a collection-type Bundle (recursively flattening Bundle resources).
     *
     * @param path The root directory to bundle.
     * @return A Bundle of all the resources in the root directory and subdirectories
     */
    fun bundle(path: String): IBaseBundle? {

        val uri: URI?
        try {
            // TODO: Should use builder.UriUtil.isUri
            if (!path.startsWith("file:/") && !path.matches("\\w+?://.*".toRegex())) {
                val file = File(path)
                uri = file.toURI()
            } else {
                uri = URI(path)
            }
        } catch (e: Exception) {
            logger.error("error parsing uri from path: $path", e)
            throw RuntimeException(e)
        }

        val files: MutableCollection<File>
        if (uri.scheme != null && uri.scheme.startsWith("jar")) {
            files = this.listJar(uri, path)
        } else {
            files = this.listDirectory(uri.path)
        }

        return this.bundleFiles(path, files)
    }

    private fun listJar(uri: URI, path: String): MutableCollection<File> {
        try {
            val fileSystem = FileSystems.newFileSystem(uri, mutableMapOf<String, Any?>())
            val jarPath = fileSystem.getPath(path)
            Files.walk(jarPath, FileVisitOption.FOLLOW_LINKS).use { walk ->
                return walk
                    .map { x -> x.toFile() }
                    .filter { x -> x.isFile }
                    .filter { x -> x.name.endsWith("json") || x.name.endsWith("xml") }
                    .toList()
                    .toMutableList()
            }
        } catch (e: Exception) {
            logger.error("error attempting to list jar: $uri")
            throw RuntimeException(e)
        }
    }

    private fun listDirectory(path: String): MutableCollection<File> {
        val resourceDirectory = File(path)
        require(resourceDirectory.absoluteFile.exists()) {
            "The specified path to resource files does not exist: $path"
        }

        if (resourceDirectory.absoluteFile.isDirectory) {
            return FileUtils.listFiles(resourceDirectory, arrayOf("xml", "json"), true)
        } else if (path.lowercase().endsWith("xml") || path.lowercase().endsWith("json")) {
            return mutableListOf(resourceDirectory)
        } else {
            throw IllegalArgumentException(
                "path was not a directory or a recognized FHIR file format (XML, JSON) : $path"
            )
        }
    }

    private fun bundleFiles(rootPath: String?, files: Collection<File>): IBaseBundle? {
        val resources = mutableListOf<IBaseResource>()

        for (f in files) {
            val resource = parseFile(f) ?: continue

            if (resource is IBaseBundle) {
                val innerResources = flatten(this.fhirContext, resource)
                resources.addAll(innerResources)
            } else {
                resources.add(resource)
            }
        }

        val bundleFactory = this.fhirContext.newBundleFactory()

        val bundleLinks = BundleLinks(rootPath, null, true, BundleTypeEnum.COLLECTION)

        bundleFactory.addRootPropertiesToBundle(
            "bundled-directory",
            bundleLinks,
            resources.size,
            null,
        )

        bundleFactory.addResourcesToBundle(
            resources,
            BundleTypeEnum.COLLECTION,
            "",
            BundleInclusionRule.BASED_ON_INCLUDES,
            null,
        )

        return bundleFactory.resourceBundle as IBaseBundle?
    }

    private fun parseFile(f: File): IBaseResource? {
        try {
            val resource = FileUtils.readFileToString(f, Charset.forName("UTF-8"))

            val selectedParser = this.selectParser(f.name)
            return selectedParser.parseResource(resource)
        } catch (e: Exception) {
            logger.warn("Error parsing resource {}: {}", f.absolutePath, e.message)
            return null
        }
    }

    private fun selectParser(filename: String): IParser {
        if (filename.lowercase().endsWith("json")) {
            if (this.json == null) {
                this.json = this.fhirContext.newJsonParser()
            }

            return this.json!!
        } else {
            if (this.xml == null) {
                this.xml = this.fhirContext.newXmlParser()
            }

            return this.xml!!
        }
    }

    private fun flatten(fhirContext: FhirContext, bundle: IBaseBundle?): List<IBaseResource> {
        val resources = mutableListOf<IBaseResource>()

        val bundleResources = BundleUtil.toListOfResources(fhirContext, bundle)
        for (r in bundleResources) {
            if (r is IBaseBundle) {
                val innerResources = flatten(fhirContext, r)
                resources.addAll(innerResources)
            } else {
                resources.add(r)
            }
        }

        return resources
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DirectoryBundler::class.java)
    }
}

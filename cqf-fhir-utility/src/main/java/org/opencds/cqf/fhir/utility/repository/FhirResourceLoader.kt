package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import java.io.File
import java.nio.file.Path
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.repository.ig.CqlContent.loadCqlContent

class FhirResourceLoader(
    var fhirContext: FhirContext?,
    var relativeToClazz: Class<*>,
    directoryList: MutableList<String?>,
    recursive: Boolean,
) : ResourceLoader {
    var resources: MutableList<IBaseResource?>?

    init {
        this.resources =
            directoryList
                .mapNotNull { relativePath -> this.getDirectoryOrFileLocation(relativePath!!) }
                .flatMap { x -> this.getFilePaths(x, recursive) }
                .map { location -> this.loadTestResources(location!!) }
                .toMutableList()
    }

    override val relativeClass: Class<*>
        get() {
            return relativeToClazz
        }

    private fun getDirectoryOrFileLocation(relativePath: String): String? {
        val resource = this.relativeToClazz.getResource(relativePath)

        if (resource == null) {
            return null
        }

        var directoryLocationUrl = resource.toString()

        if (directoryLocationUrl.startsWith("file:/")) {
            directoryLocationUrl = directoryLocationUrl.substring("file:/".length - 1)
        }
        return directoryLocationUrl
    }

    private fun getFilePaths(directoryPath: String, recursive: Boolean): MutableList<String?> {
        val filePaths = mutableListOf<String?>()
        val inputDir = File(directoryPath)
        val files =
            if (inputDir.isDirectory)
                (inputDir.listFiles() ?: throw NoSuchElementException()).toList()
            else emptyList()

        for (file in files) {
            if (file.isDirectory) {
                if (recursive) {
                    filePaths.addAll(getFilePaths(file.path, recursive))
                }
            } else {
                if (!file.path.endsWith(".cql")) filePaths.add(file.path)
            }
        }
        return filePaths
    }

    private fun loadTestResources(location: String): IBaseResource? {
        val path = Path.of(location)
        val resource = readResource(fhirContext!!, location)
        loadCqlContent(resource, path.parent)
        return resource
    }
}

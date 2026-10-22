package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.function.Supplier
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
                .stream()
                .map<String?> { relativePath: String? ->
                    this.getDirectoryOrFileLocation(relativePath!!)
                }
                .filter { x: String? -> x != null }
                .map<MutableList<String?>?> { x: String? -> this.getFilePaths(x!!, recursive) }
                .flatMap<String?> { obj: MutableList<String?>? -> obj!!.stream() }
                .map<IBaseResource?> { location: String? -> this.loadTestResources(location!!) }
                .toList()
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
        val filePaths = ArrayList<String?>()
        val inputDir = File(directoryPath)
        val files =
            if (inputDir.isDirectory())
                ArrayList<File>(
                    Arrays.asList<File?>(
                        *Optional.ofNullable<Array<File?>?>(inputDir.listFiles())
                            .orElseThrow<java.util.NoSuchElementException?>(
                                Supplier { NoSuchElementException() }
                            )
                    )
                )
            else ArrayList<File?>()

        for (file in files) {
            if (file.isDirectory()) {
                if (recursive) {
                    filePaths.addAll(getFilePaths(file.getPath(), recursive))
                }
            } else {
                if (!file.getPath().endsWith(".cql")) filePaths.add(file.getPath())
            }
        }
        return filePaths
    }

    private fun loadTestResources(location: String): IBaseResource? {
        val path = Path.of(location)
        val resource = readResource(fhirContext!!, location)
        loadCqlContent(resource, path.getParent())
        return resource
    }
}

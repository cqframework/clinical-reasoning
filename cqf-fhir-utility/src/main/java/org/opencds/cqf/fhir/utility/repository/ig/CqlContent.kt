package org.opencds.cqf.fhir.utility.repository.ig

import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.regex.Pattern
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.Libraries.getName
import org.opencds.cqf.fhir.utility.Libraries.getVersion
import org.opencds.cqf.fhir.utility.dstu3.AttachmentUtil

object CqlContent {
    @JvmStatic
    fun loadCqlContent(resource: IBaseResource?, resourcePath: Path?) {
        Objects.requireNonNull<IBaseResource?>(resource, "resource can not be null")
        Objects.requireNonNull<Path?>(resourcePath, "resourcePath can not be null")

        if ("Library" != resource!!.fhirType()) {
            return
        }

        var cqlPathExtractor: Function<IBaseResource?, String?>? = null
        var cqlContentAttacher: BiConsumer<IBaseResource?, String?>? = null
        when (resource.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 -> {
                cqlPathExtractor = Function { resource: IBaseResource? ->
                    AttachmentUtil.getCqlLocation(resource)
                }
                cqlContentAttacher = BiConsumer { resource: IBaseResource?, text: String? ->
                    AttachmentUtil.addData(resource, text)
                }
            }

            FhirVersionEnum.R4 -> {
                cqlPathExtractor = Function { resource: IBaseResource? ->
                    org.opencds.cqf.fhir.utility.r4.AttachmentUtil.getCqlLocation(resource)
                }
                cqlContentAttacher = BiConsumer { resource: IBaseResource?, text: String? ->
                    org.opencds.cqf.fhir.utility.r4.AttachmentUtil.addData(resource, text)
                }
            }

            FhirVersionEnum.R5 -> {
                cqlPathExtractor = Function { resource: IBaseResource? ->
                    org.opencds.cqf.fhir.utility.r5.AttachmentUtil.getCqlLocation(resource)
                }
                cqlContentAttacher = BiConsumer { resource: IBaseResource?, text: String? ->
                    org.opencds.cqf.fhir.utility.r5.AttachmentUtil.addData(resource, text)
                }
            }

            else ->
                throw IllegalArgumentException(
                    "Unsupported FHIR version: ${resource.structureFhirVersionEnum}"
                )
        }

        CqlContent.readAndAttachCqlContent(
            resource,
            resourcePath!!,
            cqlPathExtractor,
            cqlContentAttacher,
        )
    }

    private fun readAndAttachCqlContent(
        resource: IBaseResource,
        resourcePath: Path,
        cqlPathExtractor: Function<IBaseResource?, String?>,
        cqlContentAttacher: BiConsumer<IBaseResource?, String?>,
    ) {
        val cqlPath = cqlPathExtractor.apply(resource)
        if (cqlPath == null) {
            return
        }

        val expectedVersion = getVersion(resource)
        val expectedName = getName(resource)
        val cqlContent = getCqlContent(resourcePath, cqlPath)
        // validateContentMatchesLibrary(cqlContent, expectedVersion, expectedName);
        cqlContentAttacher.accept(resource, cqlContent)
    }

    @JvmStatic
    fun validateContentMatchesLibrary(
        cqlContent: String?,
        expectedVersion: String?,
        expectedName: String?,
    ) {
        val nameInContent = getNameFromContent(cqlContent)
        if (expectedName != null && nameInContent != null && (expectedName != nameInContent)) {
            throw ResourceNotFoundException(
                "Expected CQL content with library name $expectedName but found $nameInContent"
            )
        }

        val versionInContent = getVersionFromContent(cqlContent)
        if (
            expectedVersion != null &&
                versionInContent != null &&
                (expectedVersion != versionInContent)
        ) {
            throw ResourceNotFoundException(
                "Expected CQL content with library version $expectedVersion but found $versionInContent"
            )
        }
    }

    fun getCqlContent(rootPath: Path, relativePath: String): String {
        val path = rootPath.resolve(relativePath).normalize()
        try {
            return String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw ResourceNotFoundException("Unable to read CQL content from path: $path")
        }
    }

    fun getNameFromContent(cqlContent: String?): String? {
        // Extract library name from CQL content (e.g., "library ExampleLibrary version '1.0.0'")
        if (cqlContent.isNullOrBlank()) {
            return null
        }
        val matcher = Pattern.compile("library\\s+([\\w.]+)").matcher(cqlContent)
        return if (matcher.find()) matcher.group(1) else null
    }

    fun getVersionFromContent(cqlContent: String?): String? {
        // Extract version from CQL content (e.g., "library ExampleLibrary version '1.0.0'")
        if (cqlContent.isNullOrBlank()) {
            return null
        }
        val matcher = Pattern.compile("version\\s+'([^']+)'").matcher(cqlContent)
        return if (matcher.find()) matcher.group(1) else null
    }
}

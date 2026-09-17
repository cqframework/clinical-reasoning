package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.Reflections.getNameFunction
import org.opencds.cqf.fhir.utility.Reflections.getPrimitiveFunction
import org.opencds.cqf.fhir.utility.Reflections.getVersionFunction

/** This is a utility class for content Libraries */
object Libraries {
    private val cachedFunctions: MutableMap<FhirVersionEnum, LibraryFunctions> = ConcurrentHashMap()
    private const val LIBRARY_RESOURCE_TYPE = "Library"

    /**
     * Creates the appropriate content for a given library, library function and content type
     *
     * @param library an IBase type
     * @param libraryFunctions LibraryFunction like getContent/getVersion etc
     * @param contentType the library content type used like XML/JSON
     * @return the content
     */
    fun getContent(
        library: IBaseResource,
        libraryFunctions: LibraryFunctions,
        contentType: String?,
    ): Optional<ByteArray> {
        for (attachment in libraryFunctions.attachments!!.apply(library)!!) {
            val libraryContentType = libraryFunctions.contentType!!.apply(attachment)
            if (libraryContentType != null && libraryContentType == contentType) {
                val content = libraryFunctions.content!!.apply(attachment)
                if (content != null) {
                    return Optional.of(content)
                }
            }
        }

        return Optional.empty()
    }

    /**
     * Creates the appropriate content for a given library function and content type
     *
     * @param library an IBase type
     * @param contentType the library content type used like XML/JSON
     * @return the content
     */
    @JvmStatic
    fun getContent(library: IBaseResource, contentType: String): Optional<ByteArray> {
        require(library.fhirType() == LIBRARY_RESOURCE_TYPE)

        val libraryFunctions = Libraries.getFunctions(library)
        return getContent(library, libraryFunctions, contentType)
    }

    fun getFunctions(library: IBaseResource): LibraryFunctions {
        val fhirVersion = library.structureFhirVersionEnum
        return cachedFunctions.computeIfAbsent(fhirVersion) { obj -> Libraries.getFunctions(obj) }
    }

    fun getFunctions(fhirVersionEnum: FhirVersionEnum?): LibraryFunctions {
        val fhirContext = FhirContext.forCached(fhirVersionEnum)

        val libraryClass =
            fhirContext.getResourceDefinition(LIBRARY_RESOURCE_TYPE).implementingClass
        val attachments =
            Reflections.getFunction<IBase, MutableList<IBase>?>(libraryClass, "content")

        val contentType =
            getPrimitiveFunction<IBase, String?>(
                fhirContext.getElementDefinition("Attachment")!!.implementingClass,
                "contentType",
            )

        val content =
            getPrimitiveFunction<IBase, ByteArray?>(
                fhirContext.getElementDefinition("Attachment")!!.implementingClass,
                "data",
            )
        val version = getVersionFunction<IBase>(libraryClass)
        val name = getNameFunction<IBase>(libraryClass)
        return LibraryFunctions(attachments, contentType, content, version, name)
    }

    /**
     * Returns appropriate version for a given library IBase Resource type
     *
     * @param library an IBase type
     * @return the Library version
     */
    @JvmStatic
    fun getVersion(library: IBaseResource): String? {
        require(library.fhirType() == LIBRARY_RESOURCE_TYPE)

        val libraryFunctions = Libraries.getFunctions(library)
        return libraryFunctions.version!!.apply(library)
    }

    /**
     * Returns the name for a given library IBase Resource type
     *
     * @param library an IBase type
     * @return the Library name
     */
    @JvmStatic
    fun getName(library: IBaseResource): String? {
        require(library.fhirType() == LIBRARY_RESOURCE_TYPE)

        val libraryFunctions = Libraries.getFunctions(library)
        return libraryFunctions.name!!.apply(library)
    }

    class LibraryFunctions
    internal constructor(
        val attachments: Function<IBase, MutableList<IBase>?>?,
        val contentType: Function<IBase, String?>?,
        val content: Function<IBase, ByteArray?>?,
        val version: Function<IBase, String?>?,
        val name: Function<IBase, String?>?,
    )
}

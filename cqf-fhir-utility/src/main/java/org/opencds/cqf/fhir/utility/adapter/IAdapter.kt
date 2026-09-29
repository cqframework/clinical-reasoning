package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import java.util.Date
import org.hl7.fhir.instance.model.api.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Marker interface for HL7 Structure adapters
 *
 * @param <T> An HL7 Structure Type
 */
interface IAdapter<T : IBase> {
    /** @return returns the underlying HL7 Structure for this adapter */
    fun get(): T?

    fun fhirContext(): FhirContext?

    fun fhirVersion(): FhirVersionEnum? {
        return fhirContext()!!.version.version
    }

    val adapterFactory: IAdapterFactory?

    fun setExtension(extensions: MutableList<out IBaseExtension<*, *>?>?) {
        try {
            setValue(get(), "extension", null)
            setValue(get(), "extension", extensions)
        } catch (e: Exception) {
            // Do nothing
            logger.debug(MISSING_EXTENSION, get()!!.fhirType())
        }
    }

    fun <E : IBaseExtension<*, *>> addExtension(): E? {
        if (get() is IBaseHasExtensions) {
            @Suppress("UNCHECKED_CAST")
            return (get() as IBaseHasExtensions).addExtension() as E?
        }
        return null
    }

    fun <E : IBaseExtension<*, *>> addExtension(extension: E?) {
        try {
            setValue(get(), "extension", mutableListOf(extension))
        } catch (e: Exception) {
            // Do nothing
            logger.debug(MISSING_EXTENSION, get()!!.fhirType())
        }
    }

    fun hasExtension(): Boolean {
        return getExtension<IBaseExtension<*, *>>().isNotEmpty()
    }

    fun hasExtension(url: String?): Boolean {
        return hasExtension(get(), url)
    }

    fun <E : IBaseExtension<*, *>> getExtension(): MutableList<E?> {
        return getExtension(get())
    }

    fun <E : IBaseExtension<*, *>> getExtensionByUrl(url: String?): E? {
        return getExtensionByUrl(get(), url)
    }

    fun <E : IBaseExtension<*, *>> getExtensionsByUrl(url: String?): MutableList<E?> {
        return getExtensionsByUrl(get(), url)
    }

    fun <E : IBaseExtension<*, *>> getExtension(base: IBase?): MutableList<E?> {
        @Suppress("UNCHECKED_CAST")
        return resolvePathList(base, "extension").map { e -> e as E? }.toMutableList()
    }

    fun <E : IBaseExtension<*, *>> getExtensionsByUrl(base: IBase?, url: String?): MutableList<E?> {
        @Suppress("UNCHECKED_CAST")
        return getExtension<IBaseExtension<*, *>>(base)
            .filter { e -> e!!.url == url }
            .map { e -> e as E }
            .toMutableList()
    }

    fun <E : IBaseExtension<*, *>> getExtensionsByUrls(
        base: IBase?,
        urls: Set<String?>,
    ): MutableList<E?> {
        @Suppress("UNCHECKED_CAST")
        return getExtension<IBaseExtension<*, *>>(base)
            .filter { e -> urls.contains(e!!.url) }
            .map { e -> e as E }
            .toMutableList()
    }

    fun <E : IBaseExtension<*, *>> getExtensionByUrl(base: IBase?, url: String?): E? {
        @Suppress("UNCHECKED_CAST")
        return getExtensionsByUrl<IBaseExtension<*, *>>(base, url)
            .map { e -> e as E? }
            .firstOrNull()
    }

    fun hasExtension(base: IBase?, url: String?): Boolean {
        return getExtension<IBaseExtension<*, *>>(base).any { e -> e!!.url == url }
    }

    fun resolvePath(target: Any?, path: String): Any?

    fun resolvePathList(path: String): MutableList<IBase?> {
        return resolvePathList(get(), path)
    }

    fun resolvePathList(base: IBase?, path: String): MutableList<IBase?> {
        val pathResult = resolvePath(base, path)
        @Suppress("UNCHECKED_CAST")
        return if (pathResult is MutableList<*>) pathResult as MutableList<IBase?>
        else mutableListOf()
    }

    fun <B : IBase> resolvePathList(base: IBase?, path: String, clazz: Class<B>?): MutableList<B?> {
        @Suppress("UNCHECKED_CAST")
        return resolvePathList(base, path).map { i -> i as B? }.toMutableList()
    }

    fun resolvePathString(path: String): String? {
        return resolvePathString(get(), path)
    }

    fun resolvePathString(base: IBase?, path: String): String? {
        return when (val result = resolvePath(base, path)) {
            null -> null

            is IPrimitiveType<*> if result.value is String -> result.value as String

            is IBaseReference -> result.referenceElement.value

            is IBaseEnumeration<*> -> result.valueAsString

            else ->
                throw UnprocessableEntityException(
                    "Path ($path) on element of type (${base?.javaClass?.simpleName}) could not be resolved"
                )
        }
    }

    fun resolvePath(path: String): Any? {
        return resolvePath(get(), path)
    }

    fun <B : IBase> resolvePath(path: String, clazz: Class<B>?): B? {
        return resolvePath(get(), path, clazz)
    }

    fun <B : IBase> resolvePath(base: IBase?, path: String, clazz: Class<B>?): B? {
        @Suppress("UNCHECKED_CAST")
        return resolvePath(base, path) as B?
    }

    fun setValue(target: IBase?, path: String, value: Any?)

    fun setValue(path: String, value: Any?) {
        setValue(get(), path, value)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(IAdapter::class.java)

        @JvmStatic
        fun <T : ICompositeType> newPeriod(version: FhirVersionEnum): T {
            @Suppress("UNCHECKED_CAST")
            return when (version) {
                FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.Period()
                FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.Period()
                FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.Period()
                else -> throw UnprocessableEntityException(UNSUPPORTED_VERSION(version))
            }
                as T
        }

        @JvmStatic
        fun <T : IPrimitiveType<String?>> newStringType(
            version: FhirVersionEnum,
            string: String?,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return when (version) {
                FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.StringType(string)
                FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.StringType(string)
                FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.StringType(string)
                else -> throw UnprocessableEntityException(UNSUPPORTED_VERSION(version))
            }
                as T
        }

        @JvmStatic
        fun <T : IPrimitiveType<String?>> newUriType(version: FhirVersionEnum, string: String?): T {
            @Suppress("UNCHECKED_CAST")
            return when (version) {
                FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.UriType(string)
                FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.UriType(string)
                FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.UriType(string)
                else -> throw UnprocessableEntityException(UNSUPPORTED_VERSION(version))
            }
                as T
        }

        @JvmStatic
        fun <T : IPrimitiveType<String?>> newUrlType(version: FhirVersionEnum, string: String?): T {
            @Suppress("UNCHECKED_CAST")
            return when (version) {
                FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.UriType(string)
                FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.UrlType(string)
                FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.UrlType(string)
                else -> throw UnprocessableEntityException(UNSUPPORTED_VERSION(version))
            }
                as T
        }

        @JvmStatic
        fun <T : IPrimitiveType<Date?>> newDateType(version: FhirVersionEnum, date: Date?): T {
            @Suppress("UNCHECKED_CAST")
            return when (version) {
                FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.DateType(date)
                FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.DateType(date)
                FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.DateType(date)
                else -> throw UnprocessableEntityException(UNSUPPORTED_VERSION(version))
            }
                as T
        }

        @JvmStatic
        fun <T : IPrimitiveType<Date?>> newDateTimeType(version: FhirVersionEnum, date: Date?): T {
            @Suppress("UNCHECKED_CAST")
            return when (version) {
                FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.DateTimeType(date)
                FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.DateTimeType(date)
                FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.DateTimeType(date)
                else -> throw UnprocessableEntityException(UNSUPPORTED_VERSION(version))
            }
                as T
        }

        val UNSUPPORTED_VERSION = { v: FhirVersionEnum -> "Unsupported version: $v" }
        const val MISSING_EXTENSION: String = "Field 'extension' does not exist on Element type {}"
    }
}

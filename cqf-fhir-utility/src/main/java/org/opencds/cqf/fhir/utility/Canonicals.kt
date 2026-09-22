package org.opencds.cqf.fhir.utility

import kotlin.math.max
import kotlin.math.min
import org.hl7.fhir.instance.model.api.IPrimitiveType

object Canonicals {
    /**
     * Gets the Resource type component of a canonical url
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Resource type, or null if one can not be parsed </CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> getResourceType(
        canonicalType: CanonicalType
    ): String? {
        require(canonicalType.hasValue())

        return getResourceType(canonicalType.value!!)
    }

    /**
     * Gets the ResourceType component of a canonical url
     *
     * @param canonical the canonical url to parse
     * @return the ResourceType, or null if one can not be parsed
     */
    @JvmStatic
    fun getResourceType(canonical: String): String? {
        var canonical = canonical

        if (!canonical.contains("/")) {
            return null
        }

        // Drop only the trailing /<id> segment. Using substring rather than replace,
        // since replace is global and would mangle self-referential URLs like
        // http://hl7.org/fhir/StructureDefinition/StructureDefinition.
        canonical = canonical.substring(0, canonical.lastIndexOf("/"))
        return if (canonical.contains("/")) canonical.substring(canonical.lastIndexOf("/") + 1)
        else canonical
    }

    /**
     * Gets the ID component of a canonical url. Does not include resource name if present in the
     * url.
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Id, or null if one can not be parsed </CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> getIdPart(canonicalType: CanonicalType): String? {
        require(canonicalType.hasValue())

        return getIdPart(canonicalType.value!!)
    }

    /**
     * Gets the ID component of a canonical url. Does not include resource name if present in the
     * url.
     *
     * @param canonical the canonical url to parse
     * @return the Id, or null if one can not be parsed
     */
    @JvmStatic
    fun getIdPart(canonical: String): String? {

        val urlPart = canonical.substring(0, Canonicals.calculateLastIndex(canonical))

        if (!urlPart.contains("/")) {
            return null
        }

        return urlPart.substring(urlPart.lastIndexOf("/") + 1)
    }

    /**
     * Gets the Version component of a canonical url
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Version, or null if one can not be parsed </CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> getVersion(
        canonicalType: CanonicalType
    ): String? {
        require(canonicalType.hasValue())

        return getVersion(canonicalType.value!!)
    }

    /**
     * Gets the Version component of a canonical url
     *
     * @param canonical the canonical url to parse
     * @return the Version, or null if one can not be parsed
     */
    @JvmStatic
    fun getVersion(canonical: String): String? {

        if (!canonical.contains("|")) {
            return null
        }

        var lastIndex = canonical.lastIndexOf("#")
        if (lastIndex == -1) {
            lastIndex = canonical.length
        }

        return canonical.substring(canonical.lastIndexOf("|") + 1, lastIndex)
    }

    /**
     * Gets the Url component of a canonical url. Includes the base url, the resource type, and the
     * id if present.
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Url, or null if one can not be parsed </CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> getUrl(canonicalType: CanonicalType): String? {
        require(canonicalType.hasValue())

        return getUrl(canonicalType.value!!)
    }

    /**
     * Get the Url component of a canonical url. Includes the base url, the resource type, and the
     * id if present.
     *
     * @param canonical the canonical url to parse
     * @return the Url, or null if one can not be parsed
     */
    @JvmStatic
    fun getUrl(canonical: String): String? {

        if (
            !canonical.contains("/") &&
                !canonical.startsWith("urn:uuid") &&
                !canonical.startsWith("urn:oid")
        ) {
            return null
        }

        val lastIndex = Canonicals.calculateLastIndex(canonical)

        return canonical.substring(0, lastIndex)
    }

    /**
     * Get the Url component for a set of canonical urls. Includes the base url, the resource type,
     * and the id if present.
     *
     * @param canonicals the set of canonical urls to parse
     * @return the set of Url and null (if one can not be parsed) values
     */
    fun getUrls(canonicals: List<String>): MutableList<String?> {

        return canonicals.map { getUrl(it) }.toMutableList()
    }

    /**
     * Gets the Fragment component of a canonical url.
     *
     * @param <CanonicalType> A CanonicalType
     * @param canonicalType the canonical url to parse
     * @return the Fragment, or null if one can not be parsed </CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> getFragment(
        canonicalType: CanonicalType
    ): String? {
        require(canonicalType.hasValue())

        return getFragment(canonicalType.value!!)
    }

    /**
     * Gets the Fragment component of a canonical url.
     *
     * @param canonical the canonical url to parse
     * @return the Fragment, or null if one can not be parsed
     */
    @JvmStatic
    fun getFragment(canonical: String): String? {

        if (!canonical.contains("#")) {
            return null
        }

        return canonical.substring(canonical.lastIndexOf("#") + 1)
    }

    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> getParts(
        canonicalType: CanonicalType
    ): CanonicalParts {
        require(canonicalType.hasValue())

        return getParts(canonicalType.value!!)
    }

    @JvmStatic
    fun getParts(canonical: String): CanonicalParts {
        val url = getUrl(canonical)
        val id = getIdPart(canonical)
        val resourceType = getResourceType(canonical)
        val version = getVersion(canonical)
        val fragment = getFragment(canonical)
        return CanonicalParts(url, id, resourceType, version, fragment)
    }

    private fun calculateLastIndex(canonical: String): Int {
        val lastIndexOfBar = canonical.lastIndexOf("|")
        val lastIndexOfHash = canonical.lastIndexOf("#")

        var lastIndex = canonical.length
        val mul = lastIndexOfBar * lastIndexOfHash
        if (mul > 1) {
            lastIndex = min(lastIndexOfBar, lastIndexOfHash)
        } else if (mul < 0) {
            lastIndex = max(lastIndexOfBar, lastIndexOfHash)
        }
        return lastIndex
    }

    class CanonicalParts
    internal constructor(
        private val url: String?,
        private val idPart: String?,
        private val resourceType: String?,
        private val version: String?,
        private val fragment: String?,
    ) {
        fun url(): String? {
            return this.url
        }

        fun idPart(): String? {
            return this.idPart
        }

        fun resourceType(): String? {
            return this.resourceType
        }

        fun version(): String? {
            return this.version
        }

        fun fragment(): String? {
            return this.fragment
        }
    }
}

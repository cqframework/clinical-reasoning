package org.opencds.cqf.fhir.utility.repository.ig

import ca.uhn.fhir.rest.api.EncodingEnum
import java.util.*

/**
 * This class is used to determine how to handle encoding when reading and writing resources. You
 * can choose to preserve the encoding of the resource when reading and writing, or you can choose
 * to change the encoding to a preferred encoding when writing. New resources will always be written
 * in the preferred encoding.
 */
@JvmRecord
data class EncodingBehavior(
    val preferredEncoding: EncodingEnum?,
    val preserveEncoding: PreserveEncoding?,
    val enabledEncodings: EnumSet<EncodingEnum?>?,
) {
    /**
     * When updating a resource, you can choose to preserve the original encoding of the resource or
     * you can choose to overwrite the original encoding with the preferred encoding.
     */
    enum class PreserveEncoding {
        PRESERVE_ORIGINAL_ENCODING,
        OVERWRITE_WITH_PREFERRED_ENCODING,
    }

    constructor(
        preferredEncoding: EncodingEnum?,
        preserveEncoding: PreserveEncoding?,
    ) : this(preferredEncoding, preserveEncoding, DEFAULT_ENABLED_ENCODINGS)

    companion object {
        private val DEFAULT_ENABLED_ENCODINGS: EnumSet<EncodingEnum?> =
            EnumSet.of<EncodingEnum?>(EncodingEnum.JSON, EncodingEnum.XML)

        val DEFAULT: EncodingBehavior =
            EncodingBehavior(
                EncodingEnum.JSON,
                PreserveEncoding.PRESERVE_ORIGINAL_ENCODING,
                DEFAULT_ENABLED_ENCODINGS,
            )

        val KALM: EncodingBehavior =
            EncodingBehavior(
                EncodingEnum.JSON,
                PreserveEncoding.PRESERVE_ORIGINAL_ENCODING,
                EnumSet.of<EncodingEnum?>(EncodingEnum.JSON),
            )
    }
}

package org.opencds.cqf.fhir.utility

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.regex.Pattern

/**
 * Deterministic logical IDs for newly generated resources; never normalize supplied IDs or
 * references.
 */
object GeneratedIds {
    private val VALID_ID: Pattern = Pattern.compile("[A-Za-z0-9.-]{1,64}")
    private val INVALID_CHARACTER: Pattern = Pattern.compile("[^A-Za-z0-9.-]")
    private const val HEX = "0123456789abcdef"

    /**
     * Preserve valid IDs; otherwise retain a readable stem and a digest of the complete UTF-8
     * input.
     */
    @JvmStatic
    fun fromComposite(original: String): String {
        if (VALID_ID.matcher(original).matches()) {
            return original
        }
        val digest: ByteArray
        try {
            digest =
                MessageDigest.getInstance("SHA-256")
                    .digest(original.toByteArray(StandardCharsets.UTF_8))
        } catch (impossible: NoSuchAlgorithmException) {
            throw IllegalStateException("SHA-256 unavailable", impossible)
        }
        val hash = StringBuilder(12)
        for (i in 0..5) {
            hash.append(HEX[(digest[i].toInt() ushr 4) and 0xf])
            hash.append(HEX[digest[i].toInt() and 0xf])
        }
        var stem = INVALID_CHARACTER.matcher(original).replaceAll("")
        if (stem.length > 51) {
            stem = stem.substring(0, 51)
        }
        return if (stem.isEmpty()) hash.toString() else "$stem-$hash"
    }
}

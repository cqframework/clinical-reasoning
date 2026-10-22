package org.opencds.cqf.fhir.utility.repository.ig

import java.util.function.Supplier

/**
 * Represents the compartment directory segments that should be applied when resolving a filesystem
 * location for a resource.
 */
@JvmRecord
data class CompartmentAssignment(val compartmentType: String?, val compartmentId: String?) {
    fun hasCompartmentId(): Boolean {
        return !compartmentId.isNullOrBlank()
    }

    val isNone: Boolean
        get() = compartmentType.isNullOrBlank()

    val isUnknown: Boolean
        // Returns an "unknown" compartment assignment of the specified type.
        get() = !this.isNone && !this.isShared && !hasCompartmentId()

    val isShared: Boolean
        get() = compartmentType != null && compartmentType == SHARED_COMPARTMENT

    fun or(other: Supplier<CompartmentAssignment?>): CompartmentAssignment? {
        if (this.isNone) {
            return other.get()
        }

        return this
    }

    companion object {
        const val SHARED_COMPARTMENT: String = "shared"

        @JvmStatic
        fun none(): CompartmentAssignment {
            return CompartmentAssignment(null, null)
        }

        @JvmStatic
        fun shared(): CompartmentAssignment {
            return CompartmentAssignment(SHARED_COMPARTMENT, null)
        }

        // Returns an "unknown" compartment assignment of the specified type.
        @JvmStatic
        fun unknown(compartmentType: String): CompartmentAssignment {
            return of(compartmentType, null)
        }

        @JvmStatic
        fun of(compartmentType: String, compartmentId: String?): CompartmentAssignment {
            val normalizedType = compartmentType.lowercase()
            val normalizedId =
                if (!compartmentId.isNullOrBlank()) compartmentId.trim { it <= ' ' } else null
            return CompartmentAssignment(normalizedType, normalizedId)
        }
    }
}

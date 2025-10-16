package org.opencds.cqf.fhir.utility.repository.ig;

import java.util.Objects;

/**
 * Represents the compartment directory segments that should be applied when
 * resolving a filesystem location for a resource.
 */
record CompartmentAssignment(String compartmentType, String compartmentId) {

    static final String SHARED_COMPARTMENT = "shared";

    static CompartmentAssignment none() {
        return new CompartmentAssignment(null, null);
    }

    static CompartmentAssignment shared() {
        return new CompartmentAssignment(SHARED_COMPARTMENT, null);
    }
    // Returns an "unknown" compartment assignment of the specified type.
    static CompartmentAssignment unknown(String compartmentType) {
        return of(compartmentType, null);
    }

    static CompartmentAssignment of(String compartmentType, String compartmentId) {
        Objects.requireNonNull(compartmentType, "compartmentType cannot be null");
        var normalizedType = compartmentType.toLowerCase();
        var normalizedId = compartmentId != null && !compartmentId.isBlank() ? compartmentId.trim() : null;
        return new CompartmentAssignment(normalizedType, normalizedId);
    }

    boolean hasContextId() {
        return compartmentId != null && !compartmentId.isBlank();
    }

    boolean isPresent() {
        return compartmentType != null && !compartmentType.isBlank();
    }

    // Returns an "unknown" compartment assignment of the specified type.
    // An unknown compartment assignment has a type but no id, meaning that multiple
    // compartments of that type may need to be searched.
    boolean isUnknown() {
        return isPresent() && !isShared() && !hasContextId();
    }

    boolean isShared() {
        return compartmentType != null && compartmentType.equals(SHARED_COMPARTMENT);
    }
}

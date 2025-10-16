package org.opencds.cqf.fhir.utility.repository.ig;

import java.util.Objects;

/**
 * Represents the compartment directory segments that should be applied when
 * resolving a filesystem location for a resource.
 */
record CompartmentAssignment(String compartmentType, String compartmentId) {

    static final String SHARED_COMPARTMENT = "shared";
    public static final CompartmentAssignment NONE = new CompartmentAssignment(null, null);
    public static final CompartmentAssignment SHARED = new CompartmentAssignment(SHARED_COMPARTMENT, null);

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
        return compartmentType != null;
    }
}

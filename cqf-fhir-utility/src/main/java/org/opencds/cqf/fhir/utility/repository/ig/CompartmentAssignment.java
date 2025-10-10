package org.opencds.cqf.fhir.utility.repository.ig;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the compartment directory segments that should be applied when
 * resolving a filesystem location for a resource.
 */
record CompartmentAssignment(String compartmentSegment, String contextId) {

    static final String SHARED_SEGMENT = "shared";

    static CompartmentAssignment of(String compartmentSegment, String contextId) {
        Objects.requireNonNull(compartmentSegment, "compartmentSegment cannot be null");
        var normalizedSegment = compartmentSegment.toLowerCase();
        var normalizedId = Optional.ofNullable(contextId)
                .filter(id -> !id.isBlank())
                .map(String::trim)
                .orElse(null);
        return new CompartmentAssignment(normalizedSegment, normalizedId);
    }

    static CompartmentAssignment shared() {
        return new CompartmentAssignment(SHARED_SEGMENT, null);
    }

    boolean hasContextId() {
        return contextId != null && !contextId.isBlank();
    }
}

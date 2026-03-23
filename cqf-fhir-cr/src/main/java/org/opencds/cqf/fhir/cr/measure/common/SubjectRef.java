package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Objects;

/**
 * A parsed subject reference. Enforces the {@code Type/id} format at construction time
 * so downstream code never has to re-parse or handle malformed references.
 *
 * @param type      the FHIR resource type (e.g. "Patient", "Practitioner")
 * @param id        the resource id (e.g. "123")
 */
public record SubjectRef(String type, String id) {

    public SubjectRef {
        Objects.requireNonNull(type, "Subject type is required");
        Objects.requireNonNull(id, "Subject id is required");
    }

    /** Returns the qualified reference {@code "Type/id"}. */
    public String qualified() {
        return type + "/" + id;
    }

    /**
     * Parses a qualified reference string into a {@code SubjectRef}.
     *
     * @param qualified a string in {@code "Type/id"} format (e.g. "Patient/123")
     * @throws IllegalArgumentException if the string is not in the expected format
     */
    public static SubjectRef fromQualified(String qualified) {
        Objects.requireNonNull(qualified, "Subject reference must not be null");
        int slash = qualified.indexOf('/');
        if (slash <= 0 || slash == qualified.length() - 1) {
            throw new IllegalArgumentException(
                    "Subject reference must be in Type/id format (e.g. Patient/123), got: " + qualified);
        }
        return new SubjectRef(qualified.substring(0, slash), qualified.substring(slash + 1));
    }

    @Override
    public String toString() {
        return qualified();
    }
}

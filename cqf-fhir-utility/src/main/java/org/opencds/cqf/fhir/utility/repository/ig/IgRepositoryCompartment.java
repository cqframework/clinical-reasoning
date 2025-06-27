package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Class that represents the compartment context for a given request within {@link IgRepository} only.
 */
public class IgRepositoryCompartment {

    private final String type;
    private final String id;

    private static String typeOfContext(String context) {
        return context.split("/")[0];
    }

    private static String idOfContext(String context) {
        return context.split("/")[1];
    }

    // Empty context (i.e. no compartment context)
    public IgRepositoryCompartment() {
        this.type = null;
        this.id = null;
    }

    // Context in the format ResourceType/Id
    public IgRepositoryCompartment(String context) {
        this(typeOfContext(context), idOfContext(context));
    }

    // Context in the format type and id
    public IgRepositoryCompartment(String type, String id) {
        // Make this lowercase so the path will resolve on Linux (FYI: macOS is case-insensitive)
        this.type = requireNonNullOrEmpty("type", type).toLowerCase();
        this.id = requireNonNullOrEmpty("id", id);
    }

    public String getType() {
        return this.type;
    }

    public String getId() {
        return this.id;
    }

    public boolean isEmpty() {
        return this.type == null || this.id == null;
    }

    private static String requireNonNullOrEmpty(String name, String value) {
        requireNonNull(name, "name cannot be null");
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IgRepositoryCompartment that = (IgRepositoryCompartment) o;
        return Objects.equals(type, that.type) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", IgRepositoryCompartment.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("id='" + id + "'")
                .toString();
    }
}

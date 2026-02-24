package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.engine.runtime.Code;

/**
 * This is some hackery because most of these objects don't implement
 * hashCode or equals, meaning it's hard to detect distinct values;
 */
public class StratumValueWrapper {

    protected Object value;

    public StratumValueWrapper(Object value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return this.getKey().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }

        StratumValueWrapper other = (StratumValueWrapper) o;

        if (other.getValue() == null ^ this.getValue() == null) {
            return false;
        }

        if (other.getValue() == null && this.getValue() == null) {
            return true;
        }

        return this.getKey().equals(other.getKey());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StratumValueWrapper.class.getSimpleName() + "[", "]")
                .add("value=" + value)
                .toString();
    }

    /**
     * Sentinel value used for null stratum values.
     * This allows subjects with null stratifier results to be grouped into their own stratum.
     */
    private static final String NULL_STRATUM_VALUE = "null";

    /**
     * Sentinel value used for empty collection stratum values.
     * This allows subjects with empty list/collection stratifier results to be grouped into their own stratum.
     */
    private static final String EMPTY_STRATUM_VALUE = "empty";

    public String getKey() {
        // Handle null values - group them into a special "null" stratum
        if (value == null) {
            return NULL_STRATUM_VALUE;
        }

        // Handle empty collections - group them into a special "empty" stratum
        if (isEmptyCollection(value)) {
            return EMPTY_STRATUM_VALUE;
        }

        String key = null;
        if (value instanceof Coding coding) {
            // ASSUMPTION: We won't have different systems with the same code
            // within a given stratifier / sde
            key = joinValues("coding", coding.getCode());
        } else if (value instanceof CodeableConcept concept) {
            key = joinValues("codeable-concept", concept.getCodingFirstRep().getCode());
        } else if (value instanceof Code c) {
            key = joinValues("code", c.getCode());
        } else if (value instanceof Enum<?> e) {
            key = joinValues("enum", e.toString());
        } else if (value instanceof IPrimitiveType<?> p) {
            key = joinValues("primitive", p.getValueAsString());
        } else if (value instanceof Identifier identifier) {
            key = identifier.getValue();
        } else if (value instanceof Resource resource) {
            key = resource.getIdElement().toVersionless().getValue();
        } else {
            key = value.toString();
        }

        if (key == null) {
            throw new InvalidRequestException("found a null key for the wrapped value: %s".formatted(value));
        }

        return key;
    }

    public String getValueAsString() {
        return getValueAsString(this.value);
    }

    public String getDescription() {
        if (value == null) {
            return NULL_STRATUM_VALUE;
        }
        if (isEmptyCollection(value)) {
            return EMPTY_STRATUM_VALUE;
        }
        if (value instanceof Coding coding) {
            return coding.hasDisplay() ? coding.getDisplay() : coding.getCode();
        } else if (value instanceof CodeableConcept concept) {
            return concept.getCodingFirstRep().hasDisplay()
                    ? concept.getCodingFirstRep().getDisplay()
                    : concept.getCodingFirstRep().getCode();
        } else if (value instanceof Code c) {
            return c.getDisplay() != null ? c.getDisplay() : c.getCode();
        } else if (value instanceof Enum<?> e) {
            return e.toString();
        } else if (value instanceof IPrimitiveType<?> p) {
            return p.getValueAsString();
        } else if (value instanceof Identifier identifier) {
            return identifier.getValue();
        } else if (value instanceof Resource resource) {
            return resource.getIdElement().toVersionless().getValue();
        } else {
            return value.toString();
        }
    }

    public Object getValue() {
        return this.value;
    }

    public Class<?> getValueClass() {
        if (this.value == null) {
            return String.class;
        }

        return this.value.getClass();
    }

    private String joinValues(String... elements) {
        return String.join("-", elements);
    }

    /**
     * Check if the value is an empty collection (List, Set, Map, or other Iterable).
     * CQL's empty list "{}" evaluates to an empty collection, which should be treated
     * as a distinct stratum value rather than causing an error.
     */
    private static boolean isEmptyCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        if (value instanceof Iterable<?> iterable) {
            return !iterable.iterator().hasNext();
        }
        return false;
    }

    private String getValueAsString(Object valueInner) {
        if (valueInner == null) {
            return NULL_STRATUM_VALUE;
        }
        if (isEmptyCollection(valueInner)) {
            return EMPTY_STRATUM_VALUE;
        }
        if (valueInner instanceof Coding coding) {
            return coding.getCode();
        } else if (valueInner instanceof CodeableConcept concept) {
            return concept.getCodingFirstRep().getCode();
        } else if (valueInner instanceof Code c) {
            return c.getCode();
        } else if (valueInner instanceof Enum<?> e) {
            return e.toString();
        } else if (valueInner instanceof IPrimitiveType<?> p) {
            return p.getValueAsString();
        } else if (valueInner instanceof Identifier identifier) {
            return identifier.getValue();
        } else if (valueInner instanceof Resource resource) {
            return resource.getIdElement().toVersionless().getValue();
        } else if (valueInner instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(this::getValueAsString)
                    .limit(5) // stop a massively long string if we have a huge list
                    .collect(Collectors.joining(","));
        } else {
            return valueInner.toString();
        }
    }
}

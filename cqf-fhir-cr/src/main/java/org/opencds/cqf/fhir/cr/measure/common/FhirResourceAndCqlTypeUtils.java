package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.elm.executing.EqualEvaluator;
import org.opencds.cqf.cql.engine.runtime.CqlType;

/**
 * Utility class providing equality comparison methods for FHIR resources and CQL types.
 * <p/>
 * FHIR resources are compared by their resource type and logical ID (IdElement),
 * while CQL types are compared using their {@code equal()} method.
 * <p/>
 * This class exists to compensate for the fact that FHIR resource classes and CQL types
 * do not implement equals() and hashCode() in a way that reflects their logical identity.
 */
public class FhirResourceAndCqlTypeUtils {

    private FhirResourceAndCqlTypeUtils() {
        // static utility class
    }

    public static boolean areObjectsEqual(Object obj, Object item) {
        if (obj instanceof IBaseResource objResource && item instanceof IBaseResource itemResource) {
            return areEqualResources(objResource, itemResource);
        } else if (obj instanceof CqlType objCqlType && item instanceof CqlType itemCqlType) {
            return areEqualCqlTypes(objCqlType, itemCqlType);
        } else {
            return Objects.equals(item, obj);
        }
    }

    public static boolean areEqualResources(IBaseResource resource1, IBaseResource resource2) {
        if (resource1 == resource2) {
            return true;
        }

        if (resource1.getIdElement() == null || resource2.getIdElement() == null) {
            return false;
        }

        // In case we have IDs that are identical but different resource types,
        // e.g. Patient/123 vs Observation/123
        if (resource1.getClass() != resource2.getClass()) {
            return false;
        }

        return Objects.equals(resource1.getIdElement(), resource2.getIdElement());
    }

    public static boolean areEqualCqlTypes(CqlType cqlDate1, CqlType cqlDate2) {
        if (cqlDate1 == cqlDate2) {
            return true;
        }

        if (cqlDate1 == null || cqlDate2 == null) {
            return false;
        }

        // We're relying on all CqlTypes to implement equal() properly
        // Note this is equal(), not Object.equals()
        return Boolean.TRUE.equals(EqualEvaluator.equal(cqlDate1, cqlDate2));
    }

    public static IBaseResource castToResourceIfApplicable(Object obj) {
        if (obj instanceof IBaseResource resource) {
            return resource;
        }
        return null;
    }

    public static CqlType castToCqlTypeIfApplicable(Object obj) {
        if (obj instanceof CqlType cqlDate) {
            return cqlDate;
        }
        return null;
    }

    /**
     * Find a key in a map that matches the given key using FHIR/CQL equality semantics.
     * <p/>
     * For FHIR resources, keys are compared by resource type and logical ID.
     * For CQL types, keys are compared using their {@code equal()} method.
     * For other types, standard {@code Objects.equals()} is used.
     *
     * @param map the map to search
     * @param key the key to find a match for
     * @param <K> the key type
     * @return the matching key from the map if found, null otherwise
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <K> K findMatchingKey(Map<K, ?> map, Object key) {
        for (K existingKey : map.keySet()) {
            if (areObjectsEqual(existingKey, key)) {
                return existingKey;
            }
        }
        return null;
    }

    /**
     * Find a value in a map that matches the given value using FHIR/CQL equality semantics.
     * <p/>
     * For FHIR resources, values are compared by resource type and logical ID.
     * For CQL types, values are compared using their {@code equal()} method.
     * For other types, standard {@code Objects.equals()} is used.
     *
     * @param map the map to search
     * @param value the value to find a match for
     * @param <V> the value type
     * @return the matching value from the map if found, null otherwise
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <V> V findMatchingValue(Map<?, V> map, Object value) {
        for (V existingValue : map.values()) {
            if (areObjectsEqual(existingValue, value)) {
                return existingValue;
            }
        }
        return null;
    }
}

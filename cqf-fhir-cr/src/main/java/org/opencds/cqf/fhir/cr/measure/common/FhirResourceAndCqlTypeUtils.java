package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.elm.executing.EqualEvaluator;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Value;
import org.opencds.cqf.fhir.cql.ClassInstanceHelper;

/**
 * Utility class providing equality comparison methods for FHIR resources and CQL types.
 * <p/>
 * FHIR resources are compared by their resource type and logical ID, whether they arrive as HAPI
 * objects or as the CQL engine's native {@link ClassInstance}. Everything else is compared with CQL
 * {@code equal()} semantics, or {@code Objects.equals} for values that are neither.
 * <p/>
 * This relation is the one {@link IdentityKey} keys on, so a set or map built on those keys answers
 * {@code add}, {@code contains}, {@code remove} and {@code retainAll} with the relation implemented
 * here rather than with whatever {@code equals()} the value happens to carry.
 */
public class FhirResourceAndCqlTypeUtils {

    private FhirResourceAndCqlTypeUtils() {
        // static utility class
    }

    public static boolean areObjectsEqual(Object obj, Object item) {
        final String objIdentity = resourceIdentity(obj);
        final String itemIdentity = resourceIdentity(item);

        // A resource is only ever equal to the same resource, never to a non-resource, so one
        // identity being present settles the comparison on its own.
        if (objIdentity != null || itemIdentity != null) {
            return objIdentity != null && objIdentity.equals(itemIdentity);
        }

        if (obj instanceof Value objCqlType && item instanceof Value itemCqlType) {
            return areEqualCqlTypes(objCqlType, itemCqlType);
        }

        return Objects.equals(item, obj);
    }

    /**
     * The identity a FHIR resource is compared by: its resource type and logical id, rendered as
     * {@code Type/id}. Null for anything that is not a FHIR resource.
     * <p/>
     * A HAPI resource and the engine-native {@link ClassInstance} of the same resource yield the
     * same identity. They are the same resource, and since SDE accumulation stopped converting
     * resources eagerly the pipeline holds both forms.
     * <p/>
     * A HAPI resource with no id keeps the relation this class has always had - all id-less
     * resources of a type are one. An id-less {@code ClassInstance} has no identity at all and
     * falls back to CQL equality, which is the relation that form already had.
     */
    @Nullable
    public static String resourceIdentity(Object value) {
        if (value instanceof IBaseResource resource) {
            var idElement = resource.getIdElement();
            var idPart = idElement == null ? null : idElement.getIdPart();
            return resource.fhirType() + "/" + (idPart == null ? "" : idPart);
        }

        if (value instanceof ClassInstance classInstance && ClassInstanceHelper.isFhirResource(classInstance)) {
            return ClassInstanceHelper.getId(classInstance);
        }

        return null;
    }

    public static boolean areEqualResources(IBaseResource resource1, IBaseResource resource2) {
        if (resource1 == resource2) {
            return true;
        }

        if (resource1 == null || resource2 == null) {
            return false;
        }

        return resourceIdentity(resource1).equals(resourceIdentity(resource2));
    }

    public static boolean areEqualCqlTypes(Value cqlValue1, Value cqlValue2) {
        if (cqlValue1 == cqlValue2) {
            return true;
        }

        if (cqlValue1 == null || cqlValue2 == null) {
            return false;
        }

        // We're relying on all CqlTypes to implement equal() properly
        // Note this is equal(), not Object.equals()
        var result = EqualEvaluator.equal(cqlValue1, cqlValue2);
        return result != null && result.getValue();
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
    public static <V> V findMatchingValue(Map<?, V> map, Object value) {
        for (V existingValue : map.values()) {
            if (areObjectsEqual(existingValue, value)) {
                return existingValue;
            }
        }
        return null;
    }
}

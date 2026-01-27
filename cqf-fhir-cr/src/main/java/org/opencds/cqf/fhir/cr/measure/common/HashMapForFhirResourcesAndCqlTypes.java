package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * A HashMap implementation that uses FHIR resource identity rules when comparing resource keys/values
 * or CQL equal() for CQL types.
 * <p/>
 * This means that two resources with the same resource type and logical ID are considered
 * equal as keys, even if they are different object instances.
 * <p/>
 * This class exists strictly to compensate for the fact that FHIR resource classes and CQL types
 * do not implement equals() and hashCode() in a way that reflects their logical identity.
 *
 * @param <K> the type of keys in this map, which may or may not be a {@link IBaseResource}
 *           or a CQL type
 * @param <V> the type of values in this map, which may or may not be a {@link IBaseResource}
 *           or a CQL type
 */
@SuppressWarnings("squid:S3776")
public class HashMapForFhirResourcesAndCqlTypes<K, V> extends HashMap<K, V> {

    public HashMapForFhirResourcesAndCqlTypes() {
        super();
    }

    public HashMapForFhirResourcesAndCqlTypes(Map<K, V> map) {
        super();
        this.putAll(map);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key,
     * using FHIR/CQL equality semantics for comparison.
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     */
    @Override
    public boolean containsKey(Object key) {
        return FhirResourceAndCqlTypeUtils.findMatchingKey(this, key) != null;
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value,
     * using FHIR/CQL equality semantics for comparison.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value
     */
    @Override
    public boolean containsValue(Object value) {
        return FhirResourceAndCqlTypeUtils.findMatchingValue(this, value) != null;
    }

    /**
     * Returns the value to which the specified key is mapped, using FHIR/CQL equality
     * semantics for key comparison.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if not found
     */
    @Override
    public V get(Object key) {
        K matchingKey = FhirResourceAndCqlTypeUtils.findMatchingKey(this, key);
        if (matchingKey != null) {
            return super.get(matchingKey);
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If a key matching by FHIR/CQL equality already exists, the existing entry's value
     * is replaced with the new value.
     * <p/>
     * This follows standard Map contract: put() always succeeds and replaces existing values.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping
     */
    @Override
    public V put(K key, V value) {
        K matchingKey = FhirResourceAndCqlTypeUtils.findMatchingKey(this, key);
        if (matchingKey != null) {
            // Standard Map behavior: replace the existing entry's value, return old value
            return super.put(matchingKey, value);
        }
        return super.put(key, value);
    }

    /**
     * Removes the mapping for the specified key from this map if present,
     * using FHIR/CQL equality semantics for key comparison.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping
     */
    @Override
    public V remove(Object key) {
        K matchingKey = FhirResourceAndCqlTypeUtils.findMatchingKey(this, key);
        if (matchingKey != null) {
            return super.remove(matchingKey);
        }
        return null;
    }

    /**
     * Removes the entry for the specified key only if it is currently mapped to the specified value,
     * using FHIR/CQL equality semantics for both key and value comparison.
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     */
    @Override
    public boolean remove(Object key, Object value) {
        K matchingKey = FhirResourceAndCqlTypeUtils.findMatchingKey(this, key);
        if (matchingKey != null) {
            V existingValue = super.get(matchingKey);
            if (FhirResourceAndCqlTypeUtils.areObjectsEqual(existingValue, value)) {
                super.remove(matchingKey);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code defaultValue}
     * if this map contains no mapping for the key.
     * Uses FHIR/CQL equality semantics for key comparison.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default mapping of the key
     * @return the value to which the specified key is mapped, or {@code defaultValue} if no mapping
     */
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        final K matchingKey = FhirResourceAndCqlTypeUtils.findMatchingKey(this, key);
        return matchingKey != null ? super.get(matchingKey) : defaultValue;
    }

    /**
     * If the specified key is not already associated with a value, associates it with the given value.
     * Uses FHIR/CQL equality semantics for key comparison.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping
     */
    @Override
    public V putIfAbsent(K key, V value) {
        K matchingKey = FhirResourceAndCqlTypeUtils.findMatchingKey(this, key);
        if (matchingKey != null) {
            return super.get(matchingKey);
        }
        return super.put(key, value);
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     * Uses FHIR/CQL equality semantics for key comparison.
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null} if there was no mapping
     */
    @Override
    public V replace(K key, V value) {
        K matchingKey = FhirResourceAndCqlTypeUtils.findMatchingKey(this, key);
        if (matchingKey != null) {
            return super.put(matchingKey, value);
        }
        return null;
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     * Uses FHIR/CQL equality semantics for both key and value comparison.
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        K matchingKey = FhirResourceAndCqlTypeUtils.findMatchingKey(this, key);
        if (matchingKey != null) {
            V existingValue = super.get(matchingKey);
            if (FhirResourceAndCqlTypeUtils.areObjectsEqual(existingValue, oldValue)) {
                super.put(matchingKey, newValue);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }

        return entrySet().stream()
                .map(entry -> formatEntry(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String formatEntry(K key, V value) {
        return formatObject(key) + "=" + formatObject(value);
    }

    private String formatObject(Object obj) {
        if (obj instanceof IBaseResource resource) {
            return resource.getIdElement() != null
                    ? resource.getIdElement().getValueAsString()
                    : resource.getClass().getSimpleName() + "(null id)";
        }
        return String.valueOf(obj);
    }
}

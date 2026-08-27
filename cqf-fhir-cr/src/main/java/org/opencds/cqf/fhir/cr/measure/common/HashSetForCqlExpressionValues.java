package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * A {@link HashSet} of {@link CqlExpressionValue} that compares elements by the FHIR-resource and
 * CQL-type identity rules of their underlying value (via {@link FhirResourceAndCqlTypeUtils}).
 * <p/>
 * Sister type to {@link HashSetForFhirResourcesAndCqlTypes} for use when the population pipeline
 * stores wrappers rather than raw {@link Object}s. Two wrappers around FHIR resources with the
 * same resource type and logical ID are considered equal, even if the wrappers (or the underlying
 * resource instances) are different object instances. Same applies to CQL types via
 * {@link org.opencds.cqf.cql.engine.runtime.CqlType#equal}.
 * <p/>
 * Bucket placement still uses the wrapper's default {@code Object.hashCode()} (the wrapper
 * doesn't implement {@code equals} / {@code hashCode}), so {@code add} / {@code remove} /
 * {@code contains} / {@code retainAll} fall through to linear-time identity checks via
 * {@link FhirResourceAndCqlTypeUtils#areObjectsEqual}. This is acceptable — per-subject
 * population sets are small.
 */
@SuppressWarnings("squid:S3776")
public class HashSetForCqlExpressionValues extends HashSet<CqlExpressionValue> {

    public HashSetForCqlExpressionValues() {
        super();
    }

    public HashSetForCqlExpressionValues(Collection<CqlExpressionValue> collection) {
        super();
        for (CqlExpressionValue value : collection) {
            this.add(value);
        }
    }

    public HashSetForCqlExpressionValues(Iterable<CqlExpressionValue> iterable) {
        super();
        for (CqlExpressionValue value : iterable) {
            this.add(value);
        }
    }

    /**
     * Linear-search check that any wrapper in this set has an underlying value equal — by FHIR
     * resource / CQL type identity — to {@code other}. Accepts either a {@link CqlExpressionValue}
     * (the typical case) or a raw object (so callers can ask "does this set contain a wrapper
     * around resource X?" directly).
     */
    @Override
    public boolean contains(Object other) {
        return containsByIdentity(this, unwrap(other));
    }

    /**
     * Adds {@code newElement} only if no existing wrapper in this set has an underlying value
     * equal to {@code newElement.raw()} by FHIR identity.
     */
    @Override
    public boolean add(CqlExpressionValue newElement) {
        if (newElement == null) {
            return super.add(null);
        }
        Object newRaw = newElement.raw();
        if (newRaw == null
                || (FhirResourceAndCqlTypeUtils.castToResourceIfApplicable(newRaw) == null
                        && FhirResourceAndCqlTypeUtils.castToCqlTypeIfApplicable(newRaw) == null)) {
            return super.add(newElement);
        }
        for (CqlExpressionValue existing : this) {
            if (existing != null && FhirResourceAndCqlTypeUtils.areObjectsEqual(existing.raw(), newRaw)) {
                return false;
            }
        }
        return super.add(newElement);
    }

    /**
     * Removes the wrapper whose underlying value matches {@code removalCandidate} by FHIR
     * identity. {@code removalCandidate} may be a {@link CqlExpressionValue} or a raw resource.
     */
    @Override
    public boolean remove(Object removalCandidate) {
        Object targetRaw = unwrap(removalCandidate);
        if (targetRaw == null) {
            return super.remove(removalCandidate);
        }
        if (FhirResourceAndCqlTypeUtils.castToResourceIfApplicable(targetRaw) == null
                && FhirResourceAndCqlTypeUtils.castToCqlTypeIfApplicable(targetRaw) == null) {
            return super.remove(removalCandidate);
        }
        for (CqlExpressionValue existing : this) {
            if (existing != null && FhirResourceAndCqlTypeUtils.areObjectsEqual(existing.raw(), targetRaw)) {
                return super.remove(existing);
            }
        }
        return false;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> otherCollection) {
        Objects.requireNonNull(otherCollection);

        if (otherCollection instanceof HashSetForCqlExpressionValues) {
            return super.retainAll(otherCollection);
        }

        boolean modified = false;
        Iterator<CqlExpressionValue> it = iterator();
        while (it.hasNext()) {
            CqlExpressionValue next = it.next();
            if (!otherContains(otherCollection, next)) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    private static boolean otherContains(Collection<?> collection, CqlExpressionValue value) {
        Object raw = value == null ? null : value.raw();
        for (Object other : collection) {
            Object otherRaw = unwrap(other);
            if (FhirResourceAndCqlTypeUtils.areObjectsEqual(raw, otherRaw)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsByIdentity(Iterable<CqlExpressionValue> elements, Object targetRaw) {
        for (CqlExpressionValue existing : elements) {
            Object existingRaw = existing == null ? null : existing.raw();
            if (FhirResourceAndCqlTypeUtils.areObjectsEqual(existingRaw, targetRaw)) {
                return true;
            }
        }
        return false;
    }

    private static Object unwrap(Object o) {
        return o instanceof CqlExpressionValue v ? v.raw() : o;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        Object firstRaw = iterator().next() == null ? null : iterator().next().raw();
        if (firstRaw instanceof IBaseResource) {
            return stream()
                    .map(CqlExpressionValue::raw)
                    .filter(IBaseResource.class::isInstance)
                    .map(IBaseResource.class::cast)
                    .map(r -> r.getIdElement().getValueAsString())
                    .collect(Collectors.joining(",", "[", "]"));
        }
        return super.toString();
    }
}

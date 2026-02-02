package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.runtime.CqlType;

/**
 * A HashSet implementation that uses FHIR resource identity rules when comparing resources or
 * Cql.equal.
 * This means that two resources with the same resource type and logical ID are considered
 * equal, even if they are different object instances.
 * <p/>
 * This class exists strictly to compensate for the fact that FHIR resource classes and CQL types
 * do not implement equals() and hashCode().
 * @param <T> the type of elements in this set, which may or may not be a {@link IBaseResource}
 *           or a {@link CqlType}
 */
@SuppressWarnings("squid:S3776")
public class HashSetForFhirResourcesAndCqlTypes<T> extends HashSet<T> {

    public HashSetForFhirResourcesAndCqlTypes() {
        super();
    }

    public HashSetForFhirResourcesAndCqlTypes(Collection<T> collection) {
        super(collection);
    }

    public HashSetForFhirResourcesAndCqlTypes(Iterable<T> iterable) {
        super();
        for (T value : iterable) {
            this.add(value);
        }
    }

    public HashSetForFhirResourcesAndCqlTypes(T singleValue) {
        super();
        this.add(singleValue);
    }

    /**
     * This logic is triggered by retainAll() and removeAll(), whose behaviour we're trying
     * to modify to use FHIR resource identity rules.
     *
     * @param other object to be checked for containment in this set
     * @return true if this set contains the specified element
     */
    @Override
    public boolean contains(Object other) {
        return contains(this, other);
    }

    /**
     * If we don't override this logic, we'll get duplicate resources since the comparison to
     * existing resources in the set will be based on object identity, not FHIR resource identity
     * The default implementation calls to HashMap, which means it's not based on contains()
     * <p/>
     * This is also called from super.allAll()
     *
     * @param newElement element to be added to this set
     * @return true if this set did not already contain the specified element
     */
    @Override
    public boolean add(T newElement) {
        final IBaseResource newElementResource = FhirResourceAndCqlTypeUtils.castToResourceIfApplicable(newElement);

        if (newElementResource != null) {
            if (this.contains(newElementResource)) {
                return false;
            } else {
                return super.add(newElement);
            }
        }

        final CqlType newElementCqlType = FhirResourceAndCqlTypeUtils.castToCqlTypeIfApplicable(newElement);

        if (newElementCqlType != null) {
            if (this.contains(newElementCqlType)) {
                return false;
            } else {
                return super.add(newElement);
            }
        }

        return super.add(newElement);
    }

    /**
     * If we don't override this logic, we'll get duplicate resources since the comparison to
     * existing resources in the set will be based on object identity, not FHIR resource identity
     * The default implementation calls to HashMap, which means it's not based on contains()
     * <p/>
     * This is also called from super.removeAll()
     *
     * @param removalCandidate object to be removed from this set, if present
     * @return true if this set contained the specified element
     */
    @Override
    public boolean remove(Object removalCandidate) {
        final IBaseResource removalCandidateResource =
                FhirResourceAndCqlTypeUtils.castToResourceIfApplicable(removalCandidate);

        if (removalCandidateResource != null) {
            for (T next : this) {
                if (next instanceof IBaseResource nextResource
                        && FhirResourceAndCqlTypeUtils.areEqualResources(nextResource, removalCandidateResource)) {
                    return super.remove(nextResource);
                }
            }
            return false;
        }

        final CqlType removalCandidateCqlType = FhirResourceAndCqlTypeUtils.castToCqlTypeIfApplicable(removalCandidate);

        if (removalCandidateCqlType != null) {
            for (T next : this) {
                if (next instanceof CqlType nextCqlType
                        && FhirResourceAndCqlTypeUtils.areEqualCqlTypes(nextCqlType, removalCandidateCqlType)) {
                    return super.remove(nextCqlType);
                }
            }
            return false;
        }

        return super.remove(removalCandidate);
    }

    /**
     * If we don't override this logic, we'll get duplicate resources since the comparison to
     * existing resources in the set will be based on object identity, not FHIR resource identity
     * The default implementation calls to HashMap, which means it's not based on contains()
     * <p/>
     *
     * @param otherCollection collection containing elements to be retained in this set
     * @return true if this set changed as a result of the call
     */
    @Override
    public boolean retainAll(@Nonnull Collection<?> otherCollection) {
        Objects.requireNonNull(otherCollection);

        // Both Collections are HashSetForFhirResources, so we can use the default implementation,
        // which calls HashSetForFhirResources.contains().
        if (otherCollection instanceof HashSetForFhirResourcesAndCqlTypes) {
            return super.retainAll(otherCollection);
        }

        // Now we're dealing with another Collection, which calls its own contains() method when
        // we invoke super.retainAll()
        boolean modified = false;
        Iterator<?> it = iterator();
        while (it.hasNext()) {
            if (!contains(otherCollection, it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    private static boolean contains(Collection<?> collection, Object obj) {
        final IBaseResource otherResource = FhirResourceAndCqlTypeUtils.castToResourceIfApplicable(obj);
        final CqlType otherCqlType = FhirResourceAndCqlTypeUtils.castToCqlTypeIfApplicable(obj);

        // prevent infinite recursion
        if (otherResource != null || otherCqlType != null || collection instanceof HashSetForFhirResourcesAndCqlTypes) {
            return containsInner(collection, obj);
        }

        return collection.contains(obj);
    }

    private static boolean containsInner(Collection<?> collection, Object obj) {
        for (Object item : collection) {
            if (FhirResourceAndCqlTypeUtils.areObjectsEqual(obj, item)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        final T firstElement = iterator().next();

        if (firstElement instanceof IBaseResource) {
            return stream()
                    .map(IBaseResource.class::cast)
                    .map(IBaseResource::getIdElement)
                    .map(IPrimitiveType::getValueAsString)
                    .collect(Collectors.joining(",", "[", "]"));
        }

        return super.toString();
    }
}

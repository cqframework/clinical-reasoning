package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * A HashSet implementation that uses FHIR resource identity rules when comparing resources.
 * This means that two resources with the same resource type and logical ID are considered
 * equal, even if they are different object instances.
 * <p/>
 * This class exists strictly to compensate for the fact that FHIR resource classes do not implement
 * equals() and hashCode().
 * @param <T> the type of elements in this set, which may or may not be a {@link IBaseResource}
 */
public class HashSetForFhirResources<T> extends HashSet<T> {

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
        final IBaseResource newElementResource = castToResourceIfApplicable(newElement);

        if (newElementResource != null) {
            if (this.contains(newElementResource)) {
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
        final IBaseResource removalCandidateResource = castToResourceIfApplicable(removalCandidate);

        if (removalCandidateResource != null) {
            for (T next : this) {
                if (next instanceof IBaseResource nextResource && areEqual(nextResource, removalCandidateResource)) {
                    return super.remove(nextResource);
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
        if (otherCollection instanceof HashSetForFhirResources) {
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
        final IBaseResource otherResource = castToResourceIfApplicable(obj);

        // prevent infinite recursion
        if (otherResource != null || collection instanceof HashSetForFhirResources) {
            return containsInner(collection, obj);
        }

        return collection.contains(obj);
    }

    private static boolean containsInner(Collection<?> collection, Object obj) {
        for (Object item : collection) {
            if (obj instanceof IBaseResource objResource && item instanceof IBaseResource itemResource) {
                if (areEqual(objResource, itemResource)) {
                    return true;
                }
            } else {
                if (Objects.equals(item, obj)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static IBaseResource castToResourceIfApplicable(Object obj) {
        if (obj instanceof IBaseResource resource) {
            return resource;
        }
        return null;
    }

    private static boolean areEqual(IBaseResource resource1, IBaseResource resource2) {
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
}

package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashSet;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;

// LUKETODO:  javadoc
// LUKETODO:  unit tests
public class HashSetForFhirResources<T> extends HashSet<T> {

    // This logic is triggered by retainAll() and removeAll(), whose behaviour we're trying
    // to modify to use FHIR resource identity rules
    @Override
    public boolean contains(Object other) {
        final IBaseResource otherResource = castToResourceIfApplicable(other);

        if (otherResource != null) {
            for (T next : this) {
                if (next instanceof IBaseResource nextResource) {
                    if (areEqual(nextResource, otherResource)) {
                        return true;
                    }
                }
            }
            return false;
        }

        return super.contains(other);
    }

    // If we don't override this logic, we'll get duplicate resources since the comparison to
    // existing resources in the set will be based on object identity, not FHIR resource identity
    // The default implementation calls to HashMap, which means it's not based on contains()
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

    // If we don't override this logic, we'll get duplicate resources since the comparison to
    // existing resources in the set will be based on object identity, not FHIR resource identity
    // The default implementation calls to HashMap, which means it's not based on contains()
    @Override
    public boolean remove(Object removalCandidate) {
        final IBaseResource removalCandidateResource = castToResourceIfApplicable(removalCandidate);

        if (removalCandidateResource != null) {
            for (T next : this) {
                if (next instanceof IBaseResource nextResource) {
                    if (areEqual(nextResource, removalCandidateResource)) {
                        return super.remove(nextResource);
                    }
                }
            }
            return false;
        }
        return super.remove(removalCandidate);
    }

    private IBaseResource castToResourceIfApplicable(Object obj) {
        if (obj instanceof IBaseResource resource) {
            return resource;
        }
        return null;
    }

    private static boolean areEqual(IBaseResource resource1, IBaseResource resource2) {
        if (resource1.getIdElement() == null || resource2.getIdElement() == null) {
            return false;
        }

        if (resource1 == resource2) {
            return true;
        }

        // LUKETODO:  this might be too permissive, so debug and figure out how to tighten it up
        return Objects.equals(resource1.getIdElement(), resource2.getIdElement());
    }
}

package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashSet;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;

// LUKETODO:  javadoc
// LUKETODO:  unit tests
public class HashSetForFhirResources<T> extends HashSet<T> {

    @Override
    public boolean contains(Object other) {
        if (other instanceof IBaseResource otherResource) {
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

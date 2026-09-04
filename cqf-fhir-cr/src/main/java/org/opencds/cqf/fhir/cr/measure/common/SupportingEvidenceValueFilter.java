package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nullable;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.runtime.Tuple;
import org.opencds.cqf.cql.engine.runtime.Value;
import org.opencds.cqf.fhir.cql.ClassInstanceHelper;

/**
 * Decides whether a CQL expression result is in scope to be surfaced as supporting evidence.
 * Selection is per expression, not per leaf: a value containing anything out of scope takes the
 * whole expression out.
 */
public final class SupportingEvidenceValueFilter {

    private static final int MAX_DEPTH = 25;

    private SupportingEvidenceValueFilter() {}

    public static boolean isSupported(@Nullable Object value, FhirVersionEnum fhirVersion) {
        return isSupported(value, fhirVersion, 0);
    }

    private static boolean isSupported(@Nullable Object value, FhirVersionEnum fhirVersion, int depth) {
        if (depth > MAX_DEPTH) {
            return false;
        }

        // null is representable: it encodes as a data-absent marker
        if (value == null) {
            return true;
        }

        // Anything that is not an engine value did not come from a top-level define, e.g. the
        // accumulators FunctionEvaluationHandler merges into the per-subject results.
        if (!(value instanceof Value cqlValue)) {
            return false;
        }

        // FHIR resources encode as reference strings; FHIR elements have no reference form
        if (cqlValue instanceof ClassInstance classInstance) {
            return ClassInstanceHelper.isFhirResource(fhirVersion, classInstance);
        }

        if (cqlValue instanceof org.opencds.cqf.cql.engine.runtime.List list) {
            return allSupported(list, fhirVersion, depth);
        }

        if (cqlValue instanceof Interval interval) {
            return isSupported(interval.getLow(), fhirVersion, depth + 1)
                    && isSupported(interval.getHigh(), fhirVersion, depth + 1);
        }

        if (cqlValue instanceof Tuple tuple) {
            return allSupported(tuple.getElements().values(), fhirVersion, depth);
        }

        return true;
    }

    /** One element out of scope takes the whole container with it. */
    private static boolean allSupported(Iterable<Value> elements, FhirVersionEnum fhirVersion, int depth) {
        for (Value element : elements) {
            if (!isSupported(element, fhirVersion, depth + 1)) {
                return false;
            }
        }
        return true;
    }
}

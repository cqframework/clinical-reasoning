package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Collection;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * A Set of {@link CqlExpressionValue} that identifies elements by what they wrap.
 * <p/>
 * Sister type to {@link HashSetForFhirResourcesAndCqlTypes} for use when the population pipeline
 * stores wrappers rather than raw {@link Object}s: two wrappers around the same FHIR resource are
 * one element, and {@code contains} / {@code remove} accept either a wrapper or a raw value, so a
 * caller can ask "does this set hold resource X?" directly.
 * <p/>
 * Keying on the wrapped value is the whole of the difference from the parent, which is why this is
 * a few lines rather than a parallel implementation. It used to be a parallel implementation, and
 * the two drifted: the wrapper carries no {@code equals}, so {@code add} deduplicated by wrapper
 * identity - that is, not at all - while {@code contains} compared what was wrapped.
 */
public class HashSetForCqlExpressionValues extends HashSetForFhirResourcesAndCqlTypes<CqlExpressionValue> {

    public HashSetForCqlExpressionValues() {
        super();
    }

    public HashSetForCqlExpressionValues(Collection<CqlExpressionValue> collection) {
        super(collection);
    }

    public HashSetForCqlExpressionValues(Iterable<CqlExpressionValue> iterable) {
        super(iterable);
    }

    /**
     * Keys on the wrapped value, so a wrapper and the raw value it wraps resolve to the same key.
     */
    @Override
    IdentityKey keyFor(Object element) {
        return super.keyFor(element instanceof CqlExpressionValue value ? value.raw() : element);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        var firstElement = iterator().next();
        if (firstElement != null && firstElement.raw() instanceof IBaseResource) {
            return stream()
                    .map(CqlExpressionValue::raw)
                    .filter(IBaseResource.class::isInstance)
                    .map(IBaseResource.class::cast)
                    .map(resource -> resource.getIdElement().getValueAsString())
                    .collect(Collectors.joining(",", "[", "]"));
        }
        return super.toString();
    }
}

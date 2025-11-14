package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationConverter;

/**
 * R4 version of {@link ContinuousVariableObservationConverter}, with the singleton pattern
 * enforced by an enum.
 */
@SuppressWarnings("squid:S6548")
public enum R4ContinuousVariableObservationConverter implements ContinuousVariableObservationConverter<Quantity> {
    INSTANCE;

    @Override
    public Quantity wrapResultAsQuantity(Object result) {
        return convertToQuantity(result);
    }

    private static Quantity convertToQuantity(Object obj) {
        if (obj == null) return null;

        Quantity q = new Quantity();

        if (obj instanceof Quantity existing) {
            return existing;
        } else if (obj instanceof Number number) {
            q.setValue(number.doubleValue());
        } else if (obj instanceof String s) {
            try {
                q.setValue(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("String is not a valid number: " + s, e);
            }
        } else {
            throw new IllegalArgumentException("Cannot convert object of type " + obj.getClass() + " to Quantity");
        }

        return q;
    }
}

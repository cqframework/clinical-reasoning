package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.UUID;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
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

        // Ensure we have an ID of some kind
        final Quantity quantity =
                (Quantity) new Quantity().setId(UUID.randomUUID().toString());

        if (obj instanceof Quantity existing) {
            if (existing.getIdElement() == null) {
                // Ensure we have an ID of some kind
                existing.setIdElement(new StringType(UUID.randomUUID().toString()));
            }
            return existing;
        } else if (obj instanceof Number number) {
            quantity.setValue(number.doubleValue());
        } else if (obj instanceof String s) {
            try {
                quantity.setValue(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("String is not a valid number: " + s, e);
            }
        } else {
            throw new IllegalArgumentException("Cannot convert object of type " + obj.getClass() + " to Quantity");
        }

        return quantity;
    }
}

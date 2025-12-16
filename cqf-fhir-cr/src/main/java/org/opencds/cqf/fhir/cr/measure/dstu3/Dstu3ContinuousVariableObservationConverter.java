package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.hl7.fhir.dstu3.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationConverter;
import org.opencds.cqf.fhir.cr.measure.common.def.report.QuantityReportDef;

// Updated by Claude Sonnet 4.5 on 2025-12-02
/**
 * DSTU3-specific converter for QuantityDef to FHIR Quantity.
 *
 * This implementation only handles conversion TO DSTU3 Quantity for MeasureReport population.
 * Conversion FROM CQL results is handled in ContinuousVariableObservationHandler.
 */
@SuppressWarnings("squid:S6548")
public enum Dstu3ContinuousVariableObservationConverter implements ContinuousVariableObservationConverter<Quantity> {
    INSTANCE;

    @Override
    public Quantity convertToFhirQuantity(QuantityReportDef quantityDef) {
        if (quantityDef == null) {
            return null;
        }

        Quantity quantity = new Quantity();
        Double value = quantityDef.value();
        if (value != null) {
            quantity.setValue(value);
        }
        // Note: unit, system, code are not preserved - only the numeric value
        return quantity;
    }
}

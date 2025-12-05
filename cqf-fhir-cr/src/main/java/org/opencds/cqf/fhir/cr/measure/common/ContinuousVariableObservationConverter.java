package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.fhir.instance.model.api.ICompositeType;

// Updated by Claude Sonnet 4.5 on 2025-12-02
/**
 * Convert QuantityDef to FHIR-specific Quantity for MeasureReport population.
 *
 * Simplified interface - conversion FROM CQL results is now handled directly in
 * ContinuousVariableObservationHandler since CQL never returns FHIR Quantities.
 */
public interface ContinuousVariableObservationConverter<T extends ICompositeType> {

    /**
     * Convert QuantityDef to FHIR-specific Quantity for MeasureReport population.
     *
     * @param quantityDef the version-agnostic quantity (only contains value)
     * @return FHIR-specific Quantity (R4 or DSTU3)
     */
    T convertToFhirQuantity(QuantityDef quantityDef);
}

package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;

/**
 * One row of a {@code MEASUREOBSERVATION} accumulator: an input from the population paired with
 * the {@link QuantityDef} produced by evaluating the observation function against it.
 * <p>
 * Used in place of {@code Map<inputResource, QuantityDef>} so the data flow is self-documenting,
 * the value type is statically guaranteed (no {@code QuantityDef::isInstance} filtering downstream),
 * and the consumer sites that previously iterated {@code map.keySet()} / {@code map.values()} /
 * {@code map.entrySet()} can iterate a typed {@code List<ObservationEntry>} instead.
 * <p>
 * {@code inputResource} is typed as {@link Object} rather than {@link org.hl7.fhir.instance.model.api.IBaseResource}
 * because measure-observation population basis is not constrained to FHIR resource types —
 * primitive bases (Date, Integer, etc.) are valid and the input there is a CQL value, not a FHIR
 * resource. Consumers handle this via the existing {@code FhirResourceAndCqlTypeUtils.areObjectsEqual}
 * helper and {@code instanceof IBaseResource} guards where they need to extract resource IDs.
 */
public record ObservationEntry(
        @Nullable Object inputResource, @Nullable QuantityDef observation) {}

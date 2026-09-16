package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;

/**
 * One row of a non-subject-value stratifier function-result accumulator: an input parameter
 * passed to the stratifier function paired with the heterogeneous CQL value the function returned.
 * <p>
 * Used in place of {@code Map<inputParam, functionOutput>} so the data flow is self-documenting and
 * downstream consumers iterate a typed {@code List<FunctionResultEntry>} instead of {@code Map.Entry}.
 * <p>
 * Both {@code input} and {@code output} are typed as {@link Object}: the input may be a FHIR resource
 * or a primitive (depending on population basis), and the output is whatever the CQL function produced
 * (typically a String or Number, but the contract permits any CQL value).
 */
public record FunctionResultEntry(
        @Nullable Object input, @Nullable Object output) {}

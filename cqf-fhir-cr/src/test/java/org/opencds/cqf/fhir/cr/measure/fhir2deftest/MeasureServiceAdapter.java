package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;

/**
 * Version-agnostic adapter interface for measure evaluation services.
 * <p>
 * This interface abstracts away FHIR version-specific details, allowing a single
 * unified test DSL to work across DSTU3, R4, R5, R6, and future FHIR versions.
 * </p>
 * <p>
 * Implementations delegate to version-specific measure services (e.g., R4MeasureService,
 * Dstu3MeasureService) and handle type conversions between version-agnostic request/response
 * objects and version-specific FHIR types.
 * </p>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Thin Adapter Layer:</strong> Minimal logic, primarily type conversion</li>
 *   <li><strong>Capability Reporting:</strong> Version-specific features reported via {@link #supportsMultiMeasure()}</li>
 *   <li><strong>Shared Core Logic:</strong> Measure evaluation logic remains in version-agnostic common package</li>
 *   <li><strong>Explicit Service Selection:</strong> Allows testing single measure via multi-service</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create adapter for R4
 * MeasureServiceAdapter adapter = new R4MeasureServiceAdapter(repository, options);
 *
 * // Single-measure evaluation
 * MeasureEvaluationRequest request = MeasureEvaluationRequest.builder()
 *     .measureId("MinimalProportionMeasure")
 *     .periodStart(ZonedDateTime.parse("2024-01-01T00:00:00Z"))
 *     .periodEnd(ZonedDateTime.parse("2024-12-31T23:59:59Z"))
 *     .reportType("summary")
 *     .build();
 *
 * MeasureReportAdapter result = adapter.evaluateSingle(request);
 *
 * // Multi-measure evaluation (if supported)
 * if (adapter.supportsMultiMeasure()) {
 *     MultiMeasureEvaluationRequest multiRequest = MultiMeasureEvaluationRequest.builder()
 *         .measureIds(List.of("Measure1", "Measure2", "Measure3"))
 *         .periodStart(ZonedDateTime.parse("2024-01-01T00:00:00Z"))
 *         .periodEnd(ZonedDateTime.parse("2024-12-31T23:59:59Z"))
 *         .reportType("population")
 *         .build();
 *
 *     MultiMeasureReportAdapter multiResult = adapter.evaluateMultiple(multiRequest);
 * }
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 * @see MeasureEvaluationRequest
 * @see MultiMeasureEvaluationRequest
 * @see MeasureReportAdapter
 * @see MultiMeasureReportAdapter
 */
public interface MeasureServiceAdapter {

    /**
     * Evaluate a single measure and return a version-agnostic result wrapper.
     * <p>
     * This method delegates to the version-specific single-measure service
     * (e.g., R4MeasureService, Dstu3MeasureService) after converting the
     * request parameters to the appropriate FHIR types.
     * </p>
     *
     * @param request Version-agnostic measure evaluation request
     * @return Version-agnostic wrapper around the MeasureReport
     * @throws UnsupportedOperationException if the underlying service doesn't support single-measure evaluation
     */
    MeasureReportAdapter evaluateSingle(MeasureEvaluationRequest request);

    /**
     * Evaluate multiple measures and return a version-agnostic result wrapper.
     * <p>
     * This method delegates to the version-specific multi-measure service
     * (e.g., R4MultiMeasureService) after converting request parameters.
     * </p>
     * <p>
     * <strong>DSTU3 Note:</strong> DSTU3 does not support multi-measure evaluation.
     * DSTU3 adapters will throw {@link UnsupportedOperationException}.
     * </p>
     *
     * @param request Version-agnostic multi-measure evaluation request
     * @return Version-agnostic wrapper around the Parameters/Bundles result
     * @throws UnsupportedOperationException if the underlying service doesn't support multi-measure evaluation
     * @see #supportsMultiMeasure()
     */
    MultiMeasureReportAdapter evaluateMultiple(MultiMeasureEvaluationRequest request);

    /**
     * Check if this adapter supports multi-measure evaluation.
     * <p>
     * Returns {@code false} for DSTU3, {@code true} for R4/R5/R6.
     * </p>
     *
     * @return true if multi-measure evaluation is supported, false otherwise
     */
    boolean supportsMultiMeasure();

    /**
     * Get the MeasureEvaluationOptions used by this adapter.
     * <p>
     * This allows test frameworks to register DefCaptureCallback for Def state capture.
     * </p>
     *
     * @return the MeasureEvaluationOptions instance
     */
    MeasureEvaluationOptions getMeasureEvaluationOptions();

    /**
     * Get the FHIR version supported by this adapter.
     * <p>
     * Used for version detection and capability checks.
     * </p>
     *
     * @return the FHIR version enum (DSTU3, R4, R5, etc.)
     */
    FhirVersionEnum getFhirVersion();
}

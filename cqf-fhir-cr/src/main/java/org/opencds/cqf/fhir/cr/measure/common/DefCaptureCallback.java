package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Functional interface for capturing MeasureDef state snapshots during measure evaluation.
 * <p>
 * This callback is invoked by MeasureProcessor implementations at specific points in the
 * evaluation pipeline to allow test frameworks or analysis tools to capture immutable
 * snapshots of the MeasureDef state.
 * </p>
 * <p>
 * <strong>Usage Pattern:</strong>
 * <pre>{@code
 * // In test code:
 * MeasureEvaluationOptions options = new MeasureEvaluationOptions();
 * options.setDefCaptureCallback(measureDef -> {
 *     // Store or analyze the captured snapshot
 *     capturedDefs.put(measureDef.url(), measureDef);
 * });
 *
 * // In processor code (after processResults):
 * DefCaptureCallback callback = measureEvaluationOptions.getDefCaptureCallback();
 * if (callback != null) {
 *     callback.onDefCaptured(measureDef.createSnapshot());
 * }
 * }</pre>
 * </p>
 * <p>
 * <strong>Design Notes:</strong>
 * <ul>
 *   <li>Invoked AFTER {@code MeasureEvaluationResultHandler.processResults()}</li>
 *   <li>Invoked BEFORE {@code MeasureDefScorer.score()} (scores will be null in snapshot)</li>
 *   <li>Always receives an immutable snapshot (via {@code measureDef.createSnapshot()})</li>
 *   <li>Zero production impact when callback is null (opt-in per test)</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 * @see MeasureDef#createSnapshot()
 * @see MeasureEvaluationOptions#setDefCaptureCallback(DefCaptureCallback)
 */
@FunctionalInterface
public interface DefCaptureCallback {

    /**
     * Invoked when a MeasureDef snapshot should be captured.
     * <p>
     * The provided {@code measureDef} parameter is always an immutable snapshot created via
     * {@code measureDef.createSnapshot()}, ensuring that subsequent mutations to the original
     * MeasureDef do not affect the captured state.
     * </p>
     * <p>
     * <strong>Thread Safety:</strong> Implementations should be thread-safe if the
     * measure evaluation is multi-threaded. The callback may be invoked multiple times
     * (once per measure in multi-measure evaluation).
     * </p>
     *
     * @param measureDef An immutable snapshot of the MeasureDef at the time of capture.
     *                   Never null. All collections are deep copied, but FHIR resource
     *                   objects are shared references.
     */
    void onDefCaptured(MeasureDef measureDef);
}

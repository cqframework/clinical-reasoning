package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

/**
 * Marker interface for single-measure evaluation results.
 * <p>
 * This interface serves as a type-safe marker indicating that a single-measure
 * evaluation was performed. It contains no methods because this framework is focused
 * on Def object assertions, not FHIR MeasureReport assertions.
 * </p>
 * <p>
 * For FHIR MeasureReport assertions, use the existing {@code Measure} framework.
 * This framework is specifically designed for asserting against captured {@code MeasureDef}
 * objects (internal evaluation state).
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // This framework is for Def assertions
 * Fhir2DefUnifiedMeasureTestHandler.given()
 *     .repositoryFor("MinimalMeasureEvaluation")
 * .when()
 *     .measureId("MinimalProportionMeasure")
 *     .captureDef()
 *     .evaluate()
 * .then()
 *     .def()  // Assert against captured Def objects
 *         .hasNoErrors()
 *         .firstGroup()
 *             .population("numerator").hasSubjectCount(7);
 *
 * // For MeasureReport assertions, use existing Measure framework
 * Measure.given()
 *     .repositoryFor("MinimalMeasureEvaluation")
 * .when()
 *     .measureId("MinimalProportionMeasure")
 *     .evaluate()
 * .then()
 *     .firstGroup()  // Assert against FHIR MeasureReport
 *         .population("numerator").hasCount(7);
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 * @see MeasureServiceAdapter#evaluateSingle
 * @see MultiMeasureReportAdapter
 */
public interface MeasureReportAdapter {
    // Marker interface - no methods needed
}

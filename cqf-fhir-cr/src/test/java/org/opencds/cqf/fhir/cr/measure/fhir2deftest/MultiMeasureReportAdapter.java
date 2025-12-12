package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

/**
 * Marker interface for multi-measure evaluation results.
 * <p>
 * This interface serves as a type-safe marker indicating that a multi-measure
 * evaluation was performed. It contains no methods because this framework
 * is focused on Def object assertions, not FHIR MeasureReport assertions.
 * </p>
 * <p>
 * For FHIR MeasureReport assertions, use the existing {@code MultiMeasure} framework.
 * This framework is specifically designed for asserting against captured {@code MeasureDef}
 * objects (internal evaluation state).
 * </p>
 * <p>
 * <strong>Note:</strong> DSTU3 does not support multi-measure evaluation.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // This framework is for Def assertions
 * Fhir2DefUnifiedMeasureTestHandler.given()
 *     .repositoryFor("MinimalMeasureEvaluation")
 * .when()
 *     .measureId("Measure1")
 *     .measureId("Measure2")
 *     .captureDef()
 *     .evaluate()
 * .then()
 *     .def("http://example.com/Measure1")  // Assert against captured Def
 *         .hasNoErrors()
 *         .firstGroup()
 *             .population("numerator").hasSubjectCount(7);
 *
 * // For MeasureReport assertions, use existing MultiMeasure framework
 * MultiMeasure.given()
 *     .repositoryFor("MinimalMeasureEvaluation")
 * .when()
 *     .measureId("Measure1")
 *     .measureId("Measure2")
 *     .evaluate()
 * .then()
 *     .reports()  // Assert against FHIR MeasureReports
 *         .hasSize(2);
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 * @see MeasureServiceAdapter#evaluateMultiple
 * @see MeasureReportAdapter
 */
public interface MultiMeasureReportAdapter {
    // Marker interface - no methods needed
}

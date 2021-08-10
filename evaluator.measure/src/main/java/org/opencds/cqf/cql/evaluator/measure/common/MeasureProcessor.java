package org.opencds.cqf.cql.evaluator.measure.common;

/**
 * This interface is used to define the FHIR operations an implementation of a Measure processor can support.
 * The operation definitions themselves are derived from the FHIR Clinical Reasoning IG
 */
public interface MeasureProcessor<MeasureReportT, EndpointT, BundleT> {
    /**
     * Evaluates a Measure according to the specifications defined in the FHIR
     * Clinical Reasoning Module and the CQFMeasures IG.
     * 
     * @param url                 The canonical url of the Measure to evaluate
     * @param periodStart         The start of the Measure period
     * @param periodEnd           The end of the Measure period
     * @param reportType          The type of report to generate
     * @param subject             The subject Id to evaluate
     * @param practitioner        The practitioner Id to evaluate
     * @param lastReceivedOn      The date the report was last generated
     * @param contentEndpoint     The endpoint to use for Measure content
     * @param terminologyEndpoint The endpoint to use for Terminology content
     * @param dataEndpoint        The endpoint to use for clinical data. NOTE:
     *                            Mutually exclusive with the additionalData
     *                            parameter
     * @param additionalData      A Bundle of clinical data to use during the
     *                            evaluation.
     * @return The completed Measure report.
     */
    MeasureReportT evaluateMeasure(String url, String periodStart, String periodEnd, String reportType, String subject,
            String practitioner, String lastReceivedOn, EndpointT contentEndpoint, EndpointT terminologyEndpoint,
            EndpointT dataEndpoint, BundleT additionalData);

}

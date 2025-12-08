package org.opencds.cqf.fhir.cr.measure.fhir2deftest.dstu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.dstu3.Dstu3MeasureProcessor;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MeasureReportAdapter;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MeasureServiceAdapter;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MultiMeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MultiMeasureReportAdapter;

/**
 * DSTU3-specific implementation of MeasureServiceAdapter.
 * <p>
 * Delegates to Dstu3MeasureProcessor for single-measure evaluation.
 * <strong>Does NOT support multi-measure evaluation</strong> (DSTU3 limitation).
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * IRepository repository = // ...
 * MeasureEvaluationOptions options = MeasureEvaluationOptions.defaultOptions();
 *
 * Dstu3MeasureServiceAdapter adapter = new Dstu3MeasureServiceAdapter(repository, options);
 *
 * // Single-measure evaluation (works)
 * MeasureEvaluationRequest request = MeasureEvaluationRequest.builder()
 *     .measureId("MinimalProportionMeasure")
 *     .periodStart("2024-01-01T00:00:00Z")
 *     .periodEnd("2024-12-31T23:59:59Z")
 *     .reportType("summary")
 *     .build();
 *
 * MeasureReportAdapter result = adapter.evaluateSingle(request);
 *
 * // Multi-measure evaluation (throws UnsupportedOperationException)
 * // adapter.evaluateMultiple(...); // ERROR!
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class Dstu3MeasureServiceAdapter implements MeasureServiceAdapter {

    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final Dstu3MeasureProcessor processor;

    /**
     * Create a Dstu3MeasureServiceAdapter.
     *
     * @param repository FHIR repository for data access
     * @param measureEvaluationOptions evaluation options (includes DefCaptureCallback)
     */
    public Dstu3MeasureServiceAdapter(IRepository repository, MeasureEvaluationOptions measureEvaluationOptions) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.processor = new Dstu3MeasureProcessor(repository, measureEvaluationOptions);
    }

    @Override
    public MeasureReportAdapter evaluateSingle(MeasureEvaluationRequest request) {
        // Convert request to DSTU3 types
        IdType measureId = new IdType(request.getMeasureId());

        // Convert period dates to String format (DSTU3 uses String dates, not ZonedDateTime)
        String periodStart = convertPeriodToString(request.getPeriodStart());
        String periodEnd = convertPeriodToString(request.getPeriodEnd());

        // Convert subject to list format
        List<String> subjectIds = request.getSubject() != null ? List.of(request.getSubject()) : List.of();

        // Convert Bundle and Parameters if present
        IBaseBundle dstu3AdditionalData = convertAdditionalData(request.getAdditionalData());
        Parameters dstu3Parameters = convertParameters(request.getParameters());

        // Call Dstu3MeasureProcessor
        MeasureReport measureReport = processor.evaluateMeasure(
                measureId,
                periodStart,
                periodEnd,
                request.getReportType(),
                subjectIds,
                dstu3AdditionalData,
                dstu3Parameters);

        return new Dstu3MeasureReportAdapter();
    }

    @Override
    public MultiMeasureReportAdapter evaluateMultiple(MultiMeasureEvaluationRequest request) {
        throw new UnsupportedOperationException("DSTU3 does not support multi-measure evaluation. "
                + "Multi-measure evaluation was introduced in R4. "
                + "Please use R4MeasureServiceAdapter for multi-measure support.");
    }

    @Override
    public boolean supportsMultiMeasure() {
        return false; // DSTU3 does not support multi-measure evaluation
    }

    @Override
    public MeasureEvaluationOptions getMeasureEvaluationOptions() {
        return measureEvaluationOptions;
    }

    @Override
    public FhirVersionEnum getFhirVersion() {
        return FhirVersionEnum.DSTU3;
    }

    /**
     * Convert ZonedDateTime to String format for DSTU3 (which uses String dates).
     * <p>
     * Uses ISO_LOCAL_DATE_TIME format (without timezone) which is compatible with
     * DateHelper.resolveRequestDate(). The system default timezone will be applied
     * when parsing.
     * </p>
     *
     * @param period the ZonedDateTime to convert, may be null
     * @return String representation in ISO_LOCAL_DATE_TIME format, or null if input was null
     */
    private String convertPeriodToString(ZonedDateTime period) {
        return Optional.ofNullable(period)
                .map(zdt -> zdt.toLocalDateTime().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .orElse(null);
    }

    /**
     * Convert IBaseBundle to DSTU3 Bundle type.
     *
     * @param additionalData the bundle to convert, may be null
     * @return DSTU3 Bundle, or null if input was null
     * @throws IllegalArgumentException if additionalData is not a DSTU3 Bundle
     */
    private IBaseBundle convertAdditionalData(IBaseBundle additionalData) {
        if (additionalData == null) {
            return null;
        }

        if (additionalData instanceof org.hl7.fhir.dstu3.model.Bundle) {
            return additionalData;
        }

        throw new IllegalArgumentException("additionalData must be DSTU3 Bundle, got: "
                + additionalData.getClass().getName());
    }

    /**
     * Convert IBaseParameters to DSTU3 Parameters type.
     *
     * @param parameters the parameters to convert, may be null
     * @return DSTU3 Parameters, or null if input was null
     * @throws IllegalArgumentException if parameters is not DSTU3 Parameters
     */
    private Parameters convertParameters(org.hl7.fhir.instance.model.api.IBaseParameters parameters) {
        if (parameters == null) {
            return null;
        }

        if (parameters instanceof Parameters) {
            return (Parameters) parameters;
        }

        throw new IllegalArgumentException("parameters must be DSTU3 Parameters, got: "
                + parameters.getClass().getName());
    }
}

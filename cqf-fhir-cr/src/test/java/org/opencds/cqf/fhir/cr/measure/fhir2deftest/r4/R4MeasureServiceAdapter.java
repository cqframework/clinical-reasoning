package org.opencds.cqf.fhir.cr.measure.fhir2deftest.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MeasureReportAdapter;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MeasureServiceAdapter;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MultiMeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.MultiMeasureReportAdapter;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureService;
import org.opencds.cqf.fhir.cr.measure.r4.R4MultiMeasureService;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;

/**
 * R4-specific implementation of MeasureServiceAdapter.
 * <p>
 * Delegates to R4MeasureService for single-measure evaluation and
 * R4MultiMeasureService for multi-measure evaluation, handling type
 * conversions between version-agnostic request/response objects and
 * R4-specific FHIR types.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * IRepository repository = // ...
 * MeasureEvaluationOptions options = MeasureEvaluationOptions.defaultOptions();
 * MeasurePeriodValidator validator = new MeasurePeriodValidator();
 *
 * R4MeasureServiceAdapter adapter = new R4MeasureServiceAdapter(
 *     repository, options, validator, "http://example.com/fhir");
 *
 * // Single-measure evaluation
 * MeasureEvaluationRequest request = MeasureEvaluationRequest.builder()
 *     .measureId("MinimalProportionMeasure")
 *     .periodStart("2024-01-01T00:00:00Z")
 *     .periodEnd("2024-12-31T23:59:59Z")
 *     .reportType("summary")
 *     .build();
 *
 * MeasureReportAdapter result = adapter.evaluateSingle(request);
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class R4MeasureServiceAdapter implements MeasureServiceAdapter {

    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;

    private final R4MeasureService singleService;
    private final R4MultiMeasureService multiService;

    /**
     * Create an R4MeasureServiceAdapter.
     *
     * @param repository FHIR repository for data access
     * @param measureEvaluationOptions evaluation options (includes DefCaptureCallback)
     * @param measurePeriodValidator validator for measurement periods
     * @param serverBase server base URL for generating resource URLs
     */
    public R4MeasureServiceAdapter(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasurePeriodValidator measurePeriodValidator,
            String serverBase) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;

        // Initialize R4-specific services
        this.singleService = new R4MeasureService(repository, measureEvaluationOptions, measurePeriodValidator);
        this.multiService =
                new R4MultiMeasureService(repository, measureEvaluationOptions, serverBase, measurePeriodValidator);
    }

    @Override
    public MeasureReportAdapter evaluateSingle(MeasureEvaluationRequest request) {
        // Convert request to R4 types
        Either3<CanonicalType, IdType, Measure> measureEither = createMeasureEither(request);

        // Convert Bundle and Parameters if present
        Bundle r4AdditionalData = convertAdditionalData(request.getAdditionalData());
        Parameters r4Parameters = convertParameters(request.getParameters());

        // Call R4MeasureService
        MeasureReport measureReport = singleService.evaluate(
                measureEither,
                request.getPeriodStart(),
                request.getPeriodEnd(),
                request.getReportType(),
                request.getSubject(),
                null, // lastReceivedOn
                null, // contentEndpoint
                null, // terminologyEndpoint
                null, // dataEndpoint
                r4AdditionalData,
                r4Parameters,
                request.getProductLine(),
                request.getPractitioner());

        return new R4MeasureReportAdapter();
    }

    @Override
    public MultiMeasureReportAdapter evaluateMultiple(MultiMeasureEvaluationRequest request) {
        // Convert measure IDs to R4 IdType list
        List<IdType> measureIds =
                request.getMeasureIds().stream().map(IdType::new).toList();

        // Convert Bundle and Parameters if present
        Bundle r4AdditionalData = convertAdditionalData(request.getAdditionalData());
        Parameters r4Parameters = convertParameters(request.getParameters());

        // Call R4MultiMeasureService
        multiService.evaluate(
                measureIds,
                new ArrayList<>(request.getMeasureUrls()),
                new ArrayList<>(request.getMeasureIdentifiers()),
                request.getPeriodStart(),
                request.getPeriodEnd(),
                request.getReportType(),
                request.getSubject(),
                null, // contentEndpoint
                null, // terminologyEndpoint
                null, // dataEndpoint
                r4AdditionalData,
                r4Parameters,
                request.getProductLine(),
                request.getReporter());

        return new R4MultiMeasureReportAdapter();
    }

    @Override
    public boolean supportsMultiMeasure() {
        return true; // R4 supports multi-measure evaluation
    }

    @Override
    public MeasureEvaluationOptions getMeasureEvaluationOptions() {
        return measureEvaluationOptions;
    }

    @Override
    public FhirVersionEnum getFhirVersion() {
        return FhirVersionEnum.R4;
    }

    /**
     * Convert IBaseBundle to R4 Bundle type.
     *
     * @param additionalData the bundle to convert, may be null
     * @return R4 Bundle, or null if input was null
     * @throws IllegalArgumentException if additionalData is not an R4 Bundle
     */
    private Bundle convertAdditionalData(IBaseBundle additionalData) {
        if (additionalData == null) {
            return null;
        }

        if (additionalData instanceof Bundle) {
            return (Bundle) additionalData;
        }

        throw new IllegalArgumentException("additionalData must be R4 Bundle, got: "
                + additionalData.getClass().getName());
    }

    /**
     * Convert IBaseParameters to R4 Parameters type.
     *
     * @param parameters the parameters to convert, may be null
     * @return R4 Parameters, or null if input was null
     * @throws IllegalArgumentException if parameters is not R4 Parameters
     */
    private Parameters convertParameters(IBaseParameters parameters) {
        if (parameters == null) {
            return null;
        }

        if (parameters instanceof Parameters) {
            return (Parameters) parameters;
        }

        throw new IllegalArgumentException("parameters must be R4 Parameters, got: "
                + parameters.getClass().getName());
    }

    /**
     * Create Either3 for measure identification from request.
     * <p>
     * Converts the request's measure identification (ID or URL) into an Either3
     * that can be used with R4MeasureService.
     * </p>
     *
     * @param request the measure evaluation request
     * @return Either3 containing CanonicalType (left), IdType (middle), or Measure (right)
     * @throws IllegalArgumentException if neither measureId nor measureUrl is provided
     */
    private Either3<CanonicalType, IdType, Measure> createMeasureEither(MeasureEvaluationRequest request) {
        if (request.getMeasureId() != null) {
            return Eithers.forMiddle3(new IdType(request.getMeasureId()));
        } else if (request.getMeasureUrl() != null) {
            return Eithers.forLeft3(new CanonicalType(request.getMeasureUrl()));
        } else {
            throw new IllegalArgumentException("Either measureId or measureUrl must be provided");
        }
    }
}

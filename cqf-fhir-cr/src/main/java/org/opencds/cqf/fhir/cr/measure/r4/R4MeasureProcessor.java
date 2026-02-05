package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableListMultimap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.CompositeEvaluationResultsPerMeasure;
import org.opencds.cqf.fhir.cr.measure.common.FunctionEvaluationHandler;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationResultHandler;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorTimeUtils;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MultiLibraryIdMeasureEngineDetails;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.search.Searches;

public class R4MeasureProcessor {
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final MeasureEvaluationResultHandler measureEvaluationResultHandler;

    public R4MeasureProcessor(IRepository repository, MeasureEvaluationOptions measureEvaluationOptions) {

        this.repository = Objects.requireNonNull(repository);
        this.measureEvaluationOptions =
                measureEvaluationOptions != null ? measureEvaluationOptions : MeasureEvaluationOptions.defaultOptions();
        this.measureEvaluationResultHandler =
                new MeasureEvaluationResultHandler(this.measureEvaluationOptions, new R4PopulationBasisValidator());
    }

    // Expose this so CQL measure evaluation can use the same Repository as the one passed to the
    // processor: this may be some sort of federated proxy repository initialized at runtime
    public IRepository getRepository() {
        return repository;
    }

    public MeasureReport evaluateMeasure(
            Either3<CanonicalType, IdType, Measure> measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            MeasureEvalType evalType,
            CqlEngine context,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure) {
        return this.evaluateMeasure(
                R4MeasureServiceUtils.foldMeasure(measure, this.repository),
                periodStart,
                periodEnd,
                reportType,
                subjectIds,
                evalType,
                context,
                compositeEvaluationResultsPerMeasure);
    }

    /**
     * Evaluation method that consumes pre-calculated CQL results, Processes results, builds Measure Report
     * @param measure Measure resource
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report
     * @param subjectIds the subjectIds to process
     * @param results the pre-calculated expression results
     * @return Measure Report Object
     */
    public MeasureReport evaluateMeasureResults(
            Measure measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            @Nonnull List<String> subjectIds,
            @Nonnull Map<String, EvaluationResult> results) {

        return evaluateMeasureCaptureDef(measure, periodStart, periodEnd, reportType, subjectIds, results)
                .measureReport();
    }

    /**
     * Evaluation method that generates CQL results, Processes results, builds Measure Report
     * @param measure Measure resource
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report that defines MeasureReport Type
     * @param subjectIds the subjectIds to process
     * @param evalType the type of evaluation to process, this is an output of reportType param
     * @return Measure Report resource
     */
    public MeasureReport evaluateMeasure(
            Measure measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            MeasureEvalType evalType,
            CqlEngine context,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure) {

        return evaluateMeasureCaptureDef(
                        measure,
                        periodStart,
                        periodEnd,
                        reportType,
                        subjectIds,
                        evalType,
                        context,
                        compositeEvaluationResultsPerMeasure)
                .measureReport();
    }

    /**
     * Test-visible evaluation method that captures both MeasureDef and MeasureReport.
     * <p>
     * <strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong>
     * </p>
     * <p>
     * This method is package-private and annotated with @VisibleForTesting to support
     * test frameworks that need to assert on both pre-scoring state (MeasureDef) and
     * post-scoring state (MeasureReport).
     * </p>
     *
     * @param measure Measure resource
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report
     * @param subjectIds the subjectIds to process
     * @param results the pre-calculated expression results
     * @return MeasureDefAndR4MeasureReport containing both MeasureDef and MeasureReport
     */
    @VisibleForTesting
    MeasureDefAndR4MeasureReport evaluateMeasureCaptureDef(
            Measure measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            @Nonnull List<String> subjectIds,
            @Nonnull Map<String, EvaluationResult> results) {

        checkMeasureLibrary(measure);

        MeasureEvalType evaluationType = MeasureEvalType.getEvalType(null, reportType, subjectIds);
        // Measurement Period: operation parameter defined measurement period
        Interval measurementPeriod = buildMeasurementPeriod(periodStart, periodEnd);

        // setup MeasureDef
        var measureDef = new R4MeasureDefBuilder().build(measure);

        // Process Criteria Expression Results
        measureEvaluationResultHandler.processResults(fhirContext, results, measureDef, evaluationType);

        // Build Measure Report with Results
        MeasureReport measureReport = new R4MeasureReportBuilder()
                .build(
                        measure,
                        measureDef,
                        r4EvalTypeToReportType(evaluationType, measure),
                        measurementPeriod,
                        subjectIds);

        return new MeasureDefAndR4MeasureReport(measureDef, measureReport);
    }

    /**
     * Test-visible evaluation method that captures both MeasureDef and MeasureReport.
     * <p>
     * <strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong>
     * </p>
     * <p>
     * This overload accepts CompositeEvaluationResultsPerMeasure for multi-measure evaluation.
     * </p>
     *
     * @param measure Measure resource
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report that defines MeasureReport Type
     * @param subjectIds the subjectIds to process
     * @param evalType the type of evaluation to process
     * @param context CQL engine context
     * @param compositeEvaluationResultsPerMeasure composite evaluation results
     * @return MeasureDefAndR4MeasureReport containing both MeasureDef and MeasureReport
     */
    @VisibleForTesting
    MeasureDefAndR4MeasureReport evaluateMeasureCaptureDef(
            Measure measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            MeasureEvalType evalType,
            CqlEngine context,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure) {

        MeasureEvalType evaluationType = MeasureEvalType.getEvalType(evalType, reportType, subjectIds);

        // setup MeasureDef
        var measureDef = new R4MeasureDefBuilder().build(measure);

        final Map<String, EvaluationResult> resultForThisMeasure =
                compositeEvaluationResultsPerMeasure.processMeasureForSuccessOrFailure(measureDef);

        measureEvaluationResultHandler.processResults(fhirContext, resultForThisMeasure, measureDef, evaluationType);

        var measurementPeriod = MeasureProcessorTimeUtils.getMeasurementPeriod(periodStart, periodEnd, context);

        // Build Measure Report with Results
        MeasureReport measureReport = new R4MeasureReportBuilder()
                .build(
                        measure,
                        measureDef,
                        r4EvalTypeToReportType(evaluationType, measure),
                        measurementPeriod,
                        subjectIds);

        return new MeasureDefAndR4MeasureReport(measureDef, measureReport);
    }

    /**
     * Test-visible evaluation method that captures both MeasureDef and MeasureReport.
     * <p>
     * <strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong>
     * </p>
     * <p>
     * This overload accepts Either3 for flexible measure resolution (by URL, ID, or resource)
     * and delegates to the Measure-based overload after resolution.
     * </p>
     *
     * @param measure Either canonical URL, ID, or Measure resource
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report that defines MeasureReport Type
     * @param subjectIds the subjectIds to process
     * @param evalType the type of evaluation to process
     * @param context CQL engine context
     * @param compositeEvaluationResultsPerMeasure composite evaluation results
     * @return MeasureDefAndR4MeasureReport containing both MeasureDef and MeasureReport
     */
    @VisibleForTesting
    // LUKETODO: generate tests for this since this is used downstream
    MeasureDefAndR4MeasureReport evaluateMeasureCaptureDef(
            Either3<CanonicalType, IdType, Measure> measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            MeasureEvalType evalType,
            CqlEngine context,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure) {

        return evaluateMeasureCaptureDef(
                R4MeasureServiceUtils.foldMeasure(measure, this.repository),
                periodStart,
                periodEnd,
                reportType,
                subjectIds,
                evalType,
                context,
                compositeEvaluationResultsPerMeasure);
    }

    // LUKETODO: generate tests for this since this is used downstream
    public CompositeEvaluationResultsPerMeasure evaluateMeasureWithCqlEngine(
            List<String> subjects,
            Either3<CanonicalType, IdType, Measure> measureEither,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {

        return evaluateMultiMeasuresWithCqlEngine(
                subjects,
                List.of(R4MeasureServiceUtils.foldMeasure(measureEither, repository)),
                periodStart,
                periodEnd,
                parameters,
                context);
    }

    // LUKETODO: generate tests for this since this is used downstream
    public CompositeEvaluationResultsPerMeasure evaluateMeasureIdWithCqlEngine(
            List<String> subjects,
            IIdType measureId,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {

        return evaluateMultiMeasuresWithCqlEngine(
                subjects,
                List.of(R4MeasureServiceUtils.resolveById(measureId, repository)),
                periodStart,
                periodEnd,
                parameters,
                context);
    }

    public CompositeEvaluationResultsPerMeasure evaluateMeasureWithCqlEngine(
            List<String> subjects,
            Measure measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {

        return evaluateMultiMeasuresWithCqlEngine(
                subjects, List.of(measure), periodStart, periodEnd, parameters, context);
    }

    public CompositeEvaluationResultsPerMeasure evaluateMultiMeasureIdsWithCqlEngine(
            List<String> subjects,
            List<IdType> measureIds,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {
        return evaluateMultiMeasuresWithCqlEngine(
                subjects,
                measureIds.stream()
                        .map(IIdType::toUnqualifiedVersionless)
                        .map(id -> R4MeasureServiceUtils.resolveById(id, repository))
                        .toList(),
                periodStart,
                periodEnd,
                parameters,
                context);
    }

    public CompositeEvaluationResultsPerMeasure evaluateMultiMeasuresWithCqlEngine(
            List<String> subjects,
            List<Measure> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {

        measures.forEach(this::checkMeasureLibrary);

        var measurementPeriodParams = buildMeasurementPeriod(periodStart, periodEnd);
        var zonedMeasurementPeriod = MeasureProcessorTimeUtils.getZonedTimeZoneForEval(
                MeasureProcessorTimeUtils.getDefaultMeasurementPeriod(measurementPeriodParams, context));

        // Do this to be backwards compatible with the previous single-library evaluation:
        // Trigger first-pass validation on measure scoring as well as other aspects of the Measures
        R4MeasureDefBuilder.triggerFirstPassValidation(measures);

        // Note that we must build the LibraryEngine BEFORE we call
        // measureProcessorUtils.setMeasurementPeriod(), otherwise, we get an NPE.
        var multiLibraryIdMeasureEngineDetails = getMultiLibraryIdMeasureEngineDetails(measures);

        var measureUrls = measures.stream()
                .map(Measure::getUrl)
                .map(url -> Optional.ofNullable(url).orElse("Unknown Measure URL"))
                .toList();

        final Map<String, Object> parametersMap = resolveParameterMap(parameters);

        FunctionEvaluationHandler.preLibraryEvaluationPeriodProcessing(
                multiLibraryIdMeasureEngineDetails.getLibraryIdentifiers(),
                measureUrls,
                parametersMap,
                context,
                measurementPeriodParams);

        // populate results from Library $evaluate
        return MeasureEvaluationResultHandler.getEvaluationResults(
                subjects, zonedMeasurementPeriod, context, multiLibraryIdMeasureEngineDetails);
    }

    private MultiLibraryIdMeasureEngineDetails getMultiLibraryIdMeasureEngineDetails(List<Measure> measures) {

        var libraryIdentifiersToMeasureIds = measures.stream()
                .collect(ImmutableListMultimap.toImmutableListMultimap(
                        this::getLibraryVersionIdentifier, // key function
                        measure -> new R4MeasureDefBuilder().build(measure) // value function
                        ));

        var libraryEngine = new LibraryEngine(repository, this.measureEvaluationOptions.getEvaluationSettings());

        var builder = MultiLibraryIdMeasureEngineDetails.builder(libraryEngine);

        libraryIdentifiersToMeasureIds
                .entries()
                .forEach(entry -> builder.addLibraryIdToMeasureId(
                        new VersionedIdentifier().withId(entry.getKey().getId()), entry.getValue()));

        return builder.build();
    }

    /**
     * method used to extract appropriate Measure Report type from operation defined Evaluation Type
     * @param measureEvalType operation evaluation type
     * @param measure resource used for evaluation
     * @return report type for Measure Report
     */
    protected MeasureReportType r4EvalTypeToReportType(MeasureEvalType measureEvalType, Measure measure) {
        return switch (measureEvalType) {
            case SUBJECT -> MeasureReportType.INDIVIDUAL;
            case SUBJECTLIST -> MeasureReportType.SUBJECTLIST;
            case POPULATION -> MeasureReportType.SUMMARY;
            default -> throw new InvalidRequestException("Unsupported MeasureEvalType: %s for Measure: %s"
                    .formatted(measureEvalType.toCode(), measure.getUrl()));
        };
    }

    /**
     * method to extract Library version defined on the Measure resource
     * @param measure resource that has desired Library
     * @return version identifier of Library
     */
    private VersionedIdentifier getLibraryVersionIdentifier(Measure measure) {

        if (measure == null) {
            throw new InvalidRequestException("Measure provided is null");
        }

        if (!measure.hasLibrary() || measure.getLibrary().isEmpty()) {
            throw new InvalidRequestException(
                    "Measure %s does not have a primary library specified".formatted(measure.getUrl()));
        }

        var url = measure.getLibrary().get(0).asStringValue();

        Bundle b = this.repository.search(Bundle.class, Library.class, Searches.byCanonical(url), null);
        if (b.getEntry().isEmpty()) {
            var errorMsg = "Unable to find Library with url: %s".formatted(url);
            throw new ResourceNotFoundException(errorMsg);
        }
        return VersionedIdentifiers.forUrl(url);
    }

    protected void checkMeasureLibrary(Measure measure) {
        if (!measure.hasLibrary()) {
            throw new InvalidRequestException(
                    "Measure %s does not have a primary library specified".formatted(measure.getUrl()));
        }
    }

    /**
     * convert Parameters resource for injection into format for cql evaluation
     * @param parameters resource used to store cql parameters
     * @return mapped parameters
     */
    private Map<String, Object> resolveParameterMap(Parameters parameters) {
        if (parameters == null) {
            return Map.of();
        }

        Map<String, Object> parameterMap = new HashMap<>();
        R4FhirModelResolver modelResolver = new R4FhirModelResolver();
        parameters.getParameter().forEach(param -> {
            Object value;
            if (param.hasResource()) {
                value = param.getResource();
            } else {
                value = param.getValue();
                if (value instanceof IPrimitiveType<?> type) {
                    // TODO: handle Code, CodeableConcept, Quantity, etc
                    // resolves Date/Time values
                    value = modelResolver.toJavaPrimitive(type.getValue(), value);
                }
            }
            if (parameterMap.containsKey(param.getName())) {
                if (parameterMap.get(param.getName()) instanceof List) {
                    if (value != null) {
                        @SuppressWarnings("unchecked")
                        var list = (List<Object>) parameterMap.get(param.getName());
                        list.add(value);
                    }
                } else {
                    // We need a mutable list here, otherwise, retrieving the list above will fail with
                    // UnsupportedOperationException
                    parameterMap.put(
                            param.getName(), new ArrayList<>(Arrays.asList(parameterMap.get(param.getName()), value)));
                }
            } else {
                parameterMap.put(param.getName(), value);
            }
        });
        return parameterMap;
    }

    public Interval buildMeasurementPeriod(ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        Interval measurementPeriod = null;
        if (periodStart != null && periodEnd != null) {
            // Operation parameter defined measurementPeriod
            var helper = new R4DateHelper();
            measurementPeriod = helper.buildMeasurementPeriodInterval(periodStart, periodEnd);
        }
        return measurementPeriod;
    }
}

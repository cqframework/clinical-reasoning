package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
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
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;

public class R4MeasureProcessor {
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final SubjectProvider subjectProvider;
    private final R4MeasureServiceUtils r4MeasureServiceUtils;
    private final MeasureProcessorUtils measureProcessorUtils = new MeasureProcessorUtils();

    public R4MeasureProcessor(
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            SubjectProvider subjectProvider,
            R4MeasureServiceUtils r4MeasureServiceUtils) {
        this.repository = Objects.requireNonNull(repository);
        this.measureEvaluationOptions =
                measureEvaluationOptions != null ? measureEvaluationOptions : MeasureEvaluationOptions.defaultOptions();
        this.subjectProvider = subjectProvider;
        this.r4MeasureServiceUtils = r4MeasureServiceUtils;
    }

    public MeasureReport evaluateMeasure(
            Either3<CanonicalType, IdType, Measure> measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters) {

        var evalType = r4MeasureServiceUtils.getMeasureEvalType(reportType, subjectIds);

        var actualRepo = this.repository;
        if (additionalData != null) {
            actualRepo = new FederatedRepository(
                    this.repository, new InMemoryFhirRepository(this.repository.fhirContext(), additionalData));
        }
        var subjects = subjectProvider.getSubjects(actualRepo, subjectIds).collect(Collectors.toList());

        return this.evaluateMeasure(
                measure, periodStart, periodEnd, reportType, subjects, additionalData, parameters, evalType);
    }

    public MeasureReport evaluateMeasure(
            Either3<CanonicalType, IdType, Measure> measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters,
            MeasureEvalType evalType) {
        var m = measure.fold(this::resolveByUrl, this::resolveById, Function.identity());
        return this.evaluateMeasure(
                m, periodStart, periodEnd, reportType, subjectIds, additionalData, parameters, evalType);
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

        checkMeasureLibrary(measure);

        MeasureEvalType evaluationType = measureProcessorUtils.getEvalType(null, reportType, subjectIds);
        // Measurement Period: operation parameter defined measurement period
        Interval measurementPeriod = buildMeasurementPeriod(periodStart, periodEnd);

        // setup MeasureDef
        var measureDef = new R4MeasureDefBuilder().build(measure);

        // Process Criteria Expression Results
        measureProcessorUtils.processResults(
                results,
                measureDef,
                evaluationType,
                this.measureEvaluationOptions.getApplyScoringSetMembership(),
                new R4PopulationBasisValidator());

        // Populate populationDefs that require MeasureDef results
        // blocking certain continuous-variable Measures due to need of CQL context
        continuousVariableObservationCheck(measureDef, measure);

        // Build Measure Report with Results
        return new R4MeasureReportBuilder()
                .build(
                        measure,
                        measureDef,
                        r4EvalTypeToReportType(evaluationType, measure),
                        measurementPeriod,
                        subjectIds);
    }

    /**
     * Evaluation method that generates CQL results, Processes results, builds Measure Report
     * @param measure Measure resource
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report that defines MeasureReport Type
     * @param subjectIds the subjectIds to process
     * @param additionalData external bundle to process with results
     * @param parameters cql parameters specified in parameters resource
     * @param evalType the type of evaluation to process, this is an output of reportType param
     * @return Measure Report resource
     */
    protected MeasureReport evaluateMeasure(
            Measure measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters,
            MeasureEvalType evalType) {

        checkMeasureLibrary(measure);

        MeasureEvalType evaluationType = measureProcessorUtils.getEvalType(evalType, reportType, subjectIds);
        // Measurement Period: operation parameter defined measurement period
        Interval measurementPeriodParams = buildMeasurementPeriod(periodStart, periodEnd);

        // setup MeasureDef
        var measureDef = new R4MeasureDefBuilder().build(measure);

        // CQL Engine context
        var context = Engines.forRepository(
                this.repository, this.measureEvaluationOptions.getEvaluationSettings(), additionalData);

        var libraryVersionIdentifier = getLibraryVersionIdentifier(measure);
        // library engine setup
        var libraryEngine = getLibraryEngine(parameters, libraryVersionIdentifier, context);

        // set measurement Period from CQL if operation parameters are empty
        measureProcessorUtils.setMeasurementPeriod(measureDef, measurementPeriodParams, context);
        // extract measurement Period from CQL to pass to report Builder
        Interval measurementPeriod =
                measureProcessorUtils.getDefaultMeasurementPeriod(measurementPeriodParams, context);
        // set offset of operation parameter measurement period
        ZonedDateTime zonedMeasurementPeriod = MeasureProcessorUtils.getZonedTimeZoneForEval(measurementPeriod);
        // populate results from Library $evaluate
        var results = measureProcessorUtils.getEvaluationResults(
                subjectIds, measureDef, zonedMeasurementPeriod, context, libraryEngine, libraryVersionIdentifier);

        // Process Criteria Expression Results
        measureProcessorUtils.processResults(
                results,
                measureDef,
                evaluationType,
                this.measureEvaluationOptions.getApplyScoringSetMembership(),
                new R4PopulationBasisValidator());

        // Populate populationDefs that require MeasureDef results
        measureProcessorUtils.continuousVariableObservation(measureDef, context);

        // Build Measure Report with Results
        return new R4MeasureReportBuilder()
                .build(
                        measure,
                        measureDef,
                        r4EvalTypeToReportType(evaluationType, measure),
                        measurementPeriod,
                        subjectIds);
    }

    /**  Temporary check for Measures that are being blocked from use by evaluateResults method
     *
     * @param measureDef defined measure definition object used to capture criteria expression results
     * @param measure measure resource used for evaluation
     */
    protected void continuousVariableObservationCheck(MeasureDef measureDef, Measure measure) {
        for (GroupDef groupDef : measureDef.groups()) {
            // Measure Observation defined?
            if (groupDef.measureScoring().equals(MeasureScoring.CONTINUOUSVARIABLE)
                    && groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION) != null) {
                throw new InvalidRequestException(
                        "Measure Evaluation Mode does not have CQL engine context to support: Measure Scoring Type: %s, Measure Population Type: %s, for Measure: %s"
                                .formatted(
                                        MeasureScoring.CONTINUOUSVARIABLE,
                                        MeasurePopulationType.MEASUREOBSERVATION,
                                        measure.getUrl()));
            }
        }
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
    protected VersionedIdentifier getLibraryVersionIdentifier(Measure measure) {
        var url = measure.getLibrary().get(0).asStringValue();

        Bundle b = this.repository.search(Bundle.class, Library.class, Searches.byCanonical(url), null);
        if (b.getEntry().isEmpty()) {
            var errorMsg = "Unable to find Library with url: %s".formatted(url);
            throw new ResourceNotFoundException(errorMsg);
        }
        return VersionedIdentifiers.forUrl(url);
    }

    /**
     * method used to initialize Library engine for generating CQL results
     * @param parameters paramaters to seed for evaluation
     * @param id library versioned identifier
     * @param context cql engine context
     * @return initialized library engine
     */
    protected LibraryEngine getLibraryEngine(Parameters parameters, VersionedIdentifier id, CqlEngine context) {

        CompiledLibrary lib;
        try {
            lib = context.getEnvironment().getLibraryManager().resolveLibrary(id);
        } catch (CqlIncludeException e) {
            throw new IllegalStateException(
                    "Unable to load CQL/ELM for library: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded."
                            .formatted(id.getId()),
                    e);
        }

        context.getState().init(lib.getLibrary());

        setArgParameters(parameters, context, lib);

        return new LibraryEngine(repository, this.measureEvaluationOptions.getEvaluationSettings());
    }

    protected void checkMeasureLibrary(Measure measure) {
        if (!measure.hasLibrary()) {
            throw new InvalidRequestException(
                    "Measure %s does not have a primary library specified".formatted(measure.getUrl()));
        }
    }

    /**
     * Set parameters for included libraries
     * Note: this may not be the optimal method (e.g. libraries with the same
     * parameter name, but different values)
     * @param parameters CQL parameters passed in from operation
     * @param context CQL engine generated
     */
    protected void setArgParameters(Parameters parameters, CqlEngine context, CompiledLibrary lib) {
        if (parameters != null) {
            Map<String, Object> paramMap = resolveParameterMap(parameters);
            context.getState().setParameters(lib.getLibrary(), paramMap);

            if (lib.getLibrary().getIncludes() != null) {
                lib.getLibrary()
                        .getIncludes()
                        .getDef()
                        .forEach(includeDef -> paramMap.forEach((paramKey, paramValue) -> context.getState()
                                .setParameter(includeDef.getLocalIdentifier(), paramKey, paramValue)));
            }
        }
    }

    protected Measure resolveByUrl(CanonicalType url) {
        var parts = Canonicals.getParts(url);
        var result = this.repository.search(
                Bundle.class, Measure.class, Searches.byNameAndVersion(parts.idPart(), parts.version()));
        return (Measure) result.getEntryFirstRep().getResource();
    }

    protected Measure resolveById(IdType id) {
        return this.repository.read(Measure.class, id);
    }

    /**
     * convert Parameters resource for injection into format for cql evaluation
     * @param parameters resource used to store cql parameters
     * @return mapped parameters
     */
    private Map<String, Object> resolveParameterMap(Parameters parameters) {
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
                    parameterMap.put(param.getName(), Arrays.asList(parameterMap.get(param.getName()), value));
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

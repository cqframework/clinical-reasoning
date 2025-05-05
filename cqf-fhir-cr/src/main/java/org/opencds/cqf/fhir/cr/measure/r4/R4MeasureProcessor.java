package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.misc.NotNull;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.FunctionDef;
import org.hl7.elm.r1.IntervalTypeSpecifier;
import org.hl7.elm.r1.NamedTypeSpecifier;
import org.hl7.elm.r1.ParameterDef;
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
import org.opencds.cqf.cql.engine.execution.Libraries;
import org.opencds.cqf.cql.engine.execution.Variable;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluator;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.MEASUREPOPULATION;

public class R4MeasureProcessor {
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final SubjectProvider subjectProvider;
    private final R4MeasureServiceUtils r4MeasureServiceUtils;
    private static final Logger logger = LoggerFactory.getLogger(R4MeasureProcessor.class);
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
                m, periodStart, periodEnd, reportType, subjectIds, additionalData, parameters, evalType, false);
    }

    protected MeasureReport evaluateMeasure(
            Measure measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters,
            MeasureEvalType evalType,
        Map<String, EvaluationResult> results,
        boolean applyScoring) {

        checkMeasureLibrary(measure);

        MeasureEvalType evaluationType = getEvalType(evalType, reportType, subjectIds);
        // Measurement Period: operation parameter defined measurement period
        Interval measurementPeriod = buildMeasurementPeriod(periodStart, periodEnd);

        // setup MeasureDef
        var measureDef = new R4MeasureDefBuilder().build(measure);

        //Process Criteria Expression Results
        processResults(results, measureDef, evaluationType, applyScoring);

        // Populate populationDefs that require MeasureDef results
        // TODO JM: CLI tool is not compliant here due to requiring CQL Engine context
        continuousVariableObservationCheck(measureDef, measure);

        // Build Measure Report with Results
        return new R4MeasureReportBuilder().build(
            measure, measureDef, r4EvalTypeToReportType(evaluationType, measure), measurementPeriod, subjectIds);
    }

    protected MeasureReport evaluateMeasure(
        Measure measure,
        @Nullable ZonedDateTime periodStart,
        @Nullable ZonedDateTime periodEnd,
        String reportType,
        List<String> subjectIds,
        IBaseBundle additionalData,
        Parameters parameters,
        MeasureEvalType evalType,
        boolean applyScoring) {

        checkMeasureLibrary(measure);


        MeasureEvalType evaluationType = getEvalType(evalType, reportType, subjectIds);
        // Measurement Period: operation parameter defined measurement period
        Interval measurementPeriod = buildMeasurementPeriod(periodStart, periodEnd);

        // setup MeasureDef
        var measureDef = new R4MeasureDefBuilder().build(measure);

        // CQL Engine context
        // TODO: JM this is not compliant with CLI tool
        var context = Engines.forRepository(
            this.repository, this.measureEvaluationOptions.getEvaluationSettings(), additionalData);

        var libraryVersionIdentifier = getLibraryVersionIdentifier(measure);
        // library engine setup
        var libraryEngine = getLibraryEngine(parameters, libraryVersionIdentifier, context);
        // set measurement Period from CQL if operation parameters are empty
        setMeasurementPeriod(measureDef, measurementPeriod, context);
        // set offset of operation parameter measurement period
        ZonedDateTime zonedMeasurementPeriod = getZonedTimeZoneForEval(measurementPeriod);
        // populate results from Library $evaluate
        var results = getEvaluationResults(subjectIds, measureDef, measure, parameters, additionalData, zonedMeasurementPeriod, context, libraryEngine, libraryVersionIdentifier);

        //Process Criteria Expression Results
        processResults(results, measureDef, evaluationType, applyScoring);

        // Populate populationDefs that require MeasureDef results
        // TODO JM: CLI tool is not compliant here due to requiring CQL Engine context
        continuousVariableObservation(measureDef, context);

        // Build Measure Report with Results
        return new R4MeasureReportBuilder().build(
            measure, measureDef, r4EvalTypeToReportType(evaluationType, measure), measurementPeriod, subjectIds);
    }

    protected void processResults(Map<String, EvaluationResult> results, MeasureDef measureDef, @NotNull MeasureEvalType measureEvalType, boolean applyScoring){
        MeasureEvaluator evaluator = new MeasureEvaluator(new R4PopulationBasisValidator());
        // Populate MeasureDef using MeasureEvaluator
        for (Map.Entry<String, EvaluationResult> entry : results.entrySet()) {
            //subject
            String subjectId = entry.getKey();
            var sub = getSubjectTypeAndId(subjectId);
            var subjectIdPart = sub.getRight();
            var subjectTypePart = sub.getLeft();
            //cql results
            EvaluationResult evalResult = entry.getValue();
            try {
                //populate results into MeasureDef
                evaluator.evaluate(measureDef, measureEvalType, subjectTypePart, subjectIdPart,
                    evalResult, applyScoring);
            } catch (Exception e) {
                // Catch Exceptions from evaluation per subject, but allow rest of subjects to be processed (if
                // applicable)
                var error = String.format("Exception for subjectId: %s, Message: %s", subjectId,
                    e.getMessage());
                // Capture error for MeasureReportBuilder
                measureDef.addError(error);
                logger.error(error, e);
            }
        }
    }

    // Temporary check for evaluation that does not have CQL engine context
    // TODO: JM CLI tool requires this check
    protected void continuousVariableObservationCheck(MeasureDef measureDef, Measure measure) {
        for (GroupDef groupDef : measureDef.groups()) {
            // Measure Observation defined?
            if (groupDef.measureScoring().equals(MeasureScoring.CONTINUOUSVARIABLE) &&
                groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION) != null) {
                throw new InvalidRequestException(String.format(
                    "Measure Evaluation Mode does not have CQL engine context to support: Measure Scoring Type: %s, Measure Population Type: %s, for Measure: %s",
                    MeasureScoring.CONTINUOUSVARIABLE, MeasurePopulationType.MEASUREOBSERVATION,
                    measure.getUrl()));
            }
        }
    }

    protected void continuousVariableObservation(MeasureDef measureDef, CqlEngine context){
        // Continuous Variable?
        for(GroupDef groupDef: measureDef.groups()){
            // Measure Observation defined?
            if(groupDef.measureScoring().equals(MeasureScoring.CONTINUOUSVARIABLE) &&
                groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION)!=null){

                PopulationDef measurePopulation = groupDef.getSingle(MEASUREPOPULATION);
                PopulationDef measureObservation = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);

                // Inject MeasurePopulation results into Measure Observation Function
                for (Object resource : measurePopulation.getResources()) {
                    Object observationResult = evaluateObservationCriteria(
                        resource,
                        measureObservation.expression(),
                        measureObservation.getEvaluatedResources(),
                        groupDef.isBooleanBasis(),
                        context);
                    measureObservation.addResource(observationResult);
                }
            }
        }
    }

    protected MeasureReportType r4EvalTypeToReportType(MeasureEvalType measureEvalType, Measure measure) {
        return switch (measureEvalType) {
            case SUBJECT -> MeasureReportType.INDIVIDUAL;
            case SUBJECTLIST -> MeasureReportType.SUBJECTLIST;
            case POPULATION -> MeasureReportType.SUMMARY;
            default -> throw new InvalidRequestException(String.format(
                "Unsupported MeasureEvalType: %s for Measure: %s", measureEvalType.toCode(),
                measure.getUrl()));
        };
    }
    protected VersionedIdentifier getLibraryVersionIdentifier(Measure measure) {
        var url = measure.getLibrary().get(0).asStringValue();

        Bundle b = this.repository.search(Bundle.class, Library.class, Searches.byCanonical(url), null);
        if (b.getEntry().isEmpty()) {
            var errorMsg = String.format("Unable to find Library with url: %s", url);
            throw new ResourceNotFoundException(errorMsg);
        }
        return VersionedIdentifiers.forUrl(url);
    }

    @Nullable
    private static ZonedDateTime getZonedTimeZoneForEval(@Nullable Interval interval) {
        return Optional.ofNullable(interval)
            .map(Interval::getLow)
            .filter(DateTime.class::isInstance)
            .map(DateTime.class::cast)
            .map(DateTime::getZoneOffset)
            .map(zoneOffset -> LocalDateTime.now().atOffset(zoneOffset).toZonedDateTime())
            .orElse(null);
    }

    protected ParameterDef getMeasurementPeriodParameterDef(CqlEngine context) {
        org.hl7.elm.r1.Library lib = context.getState().getCurrentLibrary();

        if (lib.getParameters() == null
            || lib.getParameters().getDef() == null
            || lib.getParameters().getDef().isEmpty()) {
            return null;
        }

        for (ParameterDef pd : lib.getParameters().getDef()) {
            if (pd.getName().equals(MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME)) {
                return pd;
            }
        }

        return null;
    }

    protected void setMeasurementPeriod(MeasureDef measureDef, Interval measurementPeriod, CqlEngine context) {
        ParameterDef pd = this.getMeasurementPeriodParameterDef(context);
        if (pd == null) {
            logger.warn(
                "Parameter \"{}\" was not found. Unable to validate type.", MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
            context.getState().setParameter(null, MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, measurementPeriod);
            return;
        }

        if (measurementPeriod == null && pd.getDefault() == null) {
            logger.warn(
                "No default or value supplied for Parameter \"{}\". This may result in incorrect results or errors.",
                MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
            return;
        }

        // Use the default, skip validation
        if (measurementPeriod == null) {
            measurementPeriod =
                (Interval) context.getEvaluationVisitor().visitParameterDef(pd, context.getState());

            context
                .getState()
                .setParameter(null, MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, cloneIntervalWithUtc(measurementPeriod));
            return;
        }

        IntervalTypeSpecifier intervalTypeSpecifier = (IntervalTypeSpecifier) pd.getParameterTypeSpecifier();
        if (intervalTypeSpecifier == null) {
            logger.debug(
                "No ELM type information available. Unable to validate type of \"{}\"",
                MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
            context.getState().setParameter(null, MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, measurementPeriod);
            return;
        }

        NamedTypeSpecifier pointType = (NamedTypeSpecifier) intervalTypeSpecifier.getPointType();
        String targetType = pointType.getName().getLocalPart();
        Interval convertedPeriod = convertInterval(measureDef, measurementPeriod, targetType);

        context.getState().setParameter(null, MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME, convertedPeriod);
    }

    /**
     * Convert an Interval from some other timezone to UTC, including both the start and end.
     * For example, 2020-01-16T12:00:00-07:00-2020-01-16T12:59:59-07:00 becomes
     * 2020-01-16T12:00:00Z-2020-01-16T12:59:59Z
     *
     * @param interval The original interval with some offset.
     * @return The original dateTime but converted to UTC with the same local timestamp.
     */
    private static Interval cloneIntervalWithUtc(Interval interval) {
        final Object startAsObject = interval.getStart();
        final Object endAsObject = interval.getEnd();

        if (startAsObject instanceof DateTime && endAsObject instanceof DateTime) {
            return new Interval(
                cloneDateTimeWithUtc((DateTime) startAsObject),
                true,
                cloneDateTimeWithUtc((DateTime) endAsObject),
                true);
        }

        // Give up and just return the original Interval
        return interval;
    }

    /**
     * Convert a DateTime from some other timezone to UTC.
     * For example, 2020-01-16T12:00:00-07:00 becomes 2020-01-16T12:00:00Z
     *
     * @param dateTime The original dateTime with some offset.
     * @return The original dateTime but converted to UTC with the same local timestamp.
     */
    private static DateTime cloneDateTimeWithUtc(DateTime dateTime) {
        final DateTime newDateTime = new DateTime(dateTime.getDateTime().withOffsetSameLocal(
            ZoneOffset.UTC));
        newDateTime.setPrecision(dateTime.getPrecision());
        return newDateTime;
    }

    protected Interval convertInterval(MeasureDef measureDef, Interval interval, String targetType) {
        String sourceTypeQualified = interval.getPointType().getTypeName();
        String sourceType =
            sourceTypeQualified.substring(sourceTypeQualified.lastIndexOf(".") + 1, sourceTypeQualified.length());
        if (sourceType.equals(targetType)) {
            return interval;
        }

        if (sourceType.equals("DateTime") && targetType.equals("Date")) {
            logger.debug(
                "A DateTime interval was provided and a Date interval was expected. The DateTime will be truncated.");
            return new Interval(
                truncateDateTime((DateTime) interval.getLow()),
                interval.getLowClosed(),
                truncateDateTime((DateTime) interval.getHigh()),
                interval.getHighClosed());
        }

        throw new InvalidRequestException(String.format(
            "The interval type of %s did not match the expected type of %s and no conversion was possible for MeasureDef: %s.",
            sourceType, targetType, measureDef.url()));
    }

    protected Date truncateDateTime(DateTime dateTime) {
        OffsetDateTime odt = dateTime.getDateTime();
        return new Date(odt.getYear(), odt.getMonthValue(), odt.getDayOfMonth());
    }

    protected void captureEvaluatedResources(Set<Object> outEvaluatedResources, CqlEngine context) {
        if (outEvaluatedResources != null && context.getState().getEvaluatedResources() != null) {
            outEvaluatedResources.addAll(context.getState().getEvaluatedResources());
        }
        clearEvaluatedResources(context);
    }

    // reset evaluated resources followed by a context evaluation
    private void clearEvaluatedResources(CqlEngine context) {
        context.getState().clearEvaluatedResources();
    }

    protected Object evaluateObservationCriteria(
        Object resource, String criteriaExpression, Set<Object> outEvaluatedResources, boolean isBooleanBasis, CqlEngine context) {

        var ed = Libraries.resolveExpressionRef(
            criteriaExpression, context.getState().getCurrentLibrary());

        if (!(ed instanceof FunctionDef)) {
            throw new InvalidRequestException(String.format(
                "Measure observation %s does not reference a function definition", criteriaExpression));
        }

        Object result;
        context.getState().pushWindow();
        try {
            if (!isBooleanBasis) {
                // subject based observations don't have a parameter to pass in
                context.getState()
                    .push(new Variable()
                        .withName(((FunctionDef) ed).getOperand().get(0).getName())
                        .withValue(resource));
            }
            result = context.getEvaluationVisitor().visitExpression(ed.getExpression(), context.getState());
        } finally {
            context.getState().popWindow();
        }

        captureEvaluatedResources(outEvaluatedResources, context);

        return result;
    }

    protected Map<String, EvaluationResult> getEvaluationResults(List<String> subjectIds, MeasureDef measureDef, Measure measure, Parameters parameters, IBaseBundle additionalData, ZonedDateTime zonedMeasurementPeriod, CqlEngine context, LibraryEngine libraryEngine, VersionedIdentifier id) {

        Map<String, EvaluationResult> result = new HashMap<>();

        // Library $evaluate each subject
        for (String subjectId : subjectIds) {
            if (subjectId == null) {
                throw new NullPointerException("SubjectId is required in order to calculate.");
            }
            Pair<String, String> subjectInfo = this.getSubjectTypeAndId(subjectId);
            String subjectTypePart = subjectInfo.getLeft();
            String subjectIdPart = subjectInfo.getRight();
            context.getState().setContextValue(subjectTypePart, subjectIdPart);
            try {
                result.put(subjectId,
                    libraryEngine.getEvaluationResult(
                    id, subjectId, null, null, null, null, zonedMeasurementPeriod, context)
                );
            } catch (Exception e) {
                // Catch Exceptions from evaluation per subject, but allow rest of subjects to be processed (if
                // applicable)
                var error = String.format("Exception for subjectId: %s, Message: %s", subjectId, e.getMessage());
                // Capture error for MeasureReportBuilder
                measureDef.addError(error);
                logger.error(error, e);
            }
        }

        return result;
    }

    protected Pair<String, String> getSubjectTypeAndId(String subjectId) {
        if (subjectId.contains("/")) {
            String[] subjectIdParts = subjectId.split("/");
            return Pair.of(subjectIdParts[0], subjectIdParts[1]);
        } else {
            throw new InvalidRequestException(String.format(
                "Unable to determine Subject type for id: %s. SubjectIds must be in the format {subjectType}/{subjectId} (e.g. Patient/123)",
                subjectId));
        }
    }

    protected LibraryEngine getLibraryEngine(Parameters parameters, VersionedIdentifier id, CqlEngine context) {

        CompiledLibrary lib;
        try {
            lib = context.getEnvironment().getLibraryManager().resolveLibrary(id);
        } catch (CqlIncludeException e) {
            throw new IllegalStateException(
                String.format(
                    "Unable to load CQL/ELM for library: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded.",
                    id.getId()),
                e);
        }

        context.getState().init(lib.getLibrary());

        setArgParameters(parameters, context, lib);

        return new LibraryEngine(repository, this.measureEvaluationOptions.getEvaluationSettings());
    }

    protected void checkMeasureLibrary(Measure measure){
        if (!measure.hasLibrary()) {
            throw new InvalidRequestException(
                String.format("Measure %s does not have a primary library specified", measure.getUrl()));
        }
    }

    protected MeasureEvalType getEvalType(MeasureEvalType evalType, String reportType, List<String> subjectIds){
        if (evalType == null) {
            evalType = MeasureEvalType.fromCode(reportType)
                .orElse(
                    subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
                        ? MeasureEvalType.POPULATION
                        : MeasureEvalType.SUBJECT);
        }
        return evalType;
    }

    protected Interval buildMeasurementPeriod(ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        Interval measurementPeriod = null;
        if (periodStart != null && periodEnd != null) {
            // Operation parameter defined measurementPeriod
            var helper = new R4DateHelper();
            measurementPeriod = helper.buildMeasurementPeriodInterval(periodStart, periodEnd);
        }
        return measurementPeriod;
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

    private Map<String, Object> resolveParameterMap(Parameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();
        R4FhirModelResolver modelResolver = new R4FhirModelResolver();
        parameters.getParameter().forEach(param -> {
            Object value;
            if (param.hasResource()) {
                value = param.getResource();
            } else {
                value = param.getValue();
                if (value instanceof IPrimitiveType) {
                    // TODO: handle Code, CodeableConcept, Quantity, etc
                    // resolves Date/Time values
                    value = modelResolver.toJavaPrimitive(((IPrimitiveType<?>) value).getValue(), value);
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
}

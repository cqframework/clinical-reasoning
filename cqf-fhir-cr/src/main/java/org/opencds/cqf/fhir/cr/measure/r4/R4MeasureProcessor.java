package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
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
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
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
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.MultiLibraryIdMeasureEngineDetails;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.npm.MeasureOrNpmResourceHolder;
import org.opencds.cqf.fhir.utility.npm.MeasurePlusNpmResourceHolderList;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.search.Searches;

public class R4MeasureProcessor {
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasureProcessorUtils measureProcessorUtils;
    private final R4MeasureServiceUtils r4MeasureServiceUtils;
    private final NpmPackageLoader npmPackageLoader;

    public R4MeasureProcessor(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasureProcessorUtils measureProcessorUtils,
            R4MeasureServiceUtils r4MeasureServiceUtils,
            NpmPackageLoader npmPackageLoader) {

        this.repository = Objects.requireNonNull(repository);
        this.measureEvaluationOptions =
                measureEvaluationOptions != null ? measureEvaluationOptions : MeasureEvaluationOptions.defaultOptions();
        this.measureProcessorUtils = measureProcessorUtils;
        this.r4MeasureServiceUtils = r4MeasureServiceUtils;
        this.npmPackageLoader = npmPackageLoader;
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

        var measurePlusNpmResourceHolder = r4MeasureServiceUtils.foldMeasure(measure);

        return this.evaluateMeasure(
                measurePlusNpmResourceHolder,
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
     * @param measureOrNpmResourceHolder Measure resource or NPM resource holder
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report that defines MeasureReport Type
     * @param subjectIds the subjectIds to process
     * @param evalType the type of evaluation to process, this is an output of reportType param
     * @return Measure Report resource
     */
    public MeasureReport evaluateMeasure(
            MeasureOrNpmResourceHolder measureOrNpmResourceHolder,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            MeasureEvalType evalType,
            CqlEngine context,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure) {

        MeasureEvalType evaluationType = measureProcessorUtils.getEvalType(evalType, reportType, subjectIds);

        var measure = measureOrNpmResourceHolder.getMeasure();

        // setup MeasureDef
        var measureDef = new R4MeasureDefBuilder().build(measure);

        // Process Criteria Expression Results
        final IIdType measureId = measure.getIdElement().toUnqualifiedVersionless();
        // populate results from Library $evaluate
        final Map<String, EvaluationResult> resultForThisMeasure =
                compositeEvaluationResultsPerMeasure.processMeasureForSuccessOrFailure(measureId, measureDef);

        measureProcessorUtils.processResults(
                resultForThisMeasure,
                measureDef,
                evaluationType,
                this.measureEvaluationOptions.getApplyScoringSetMembership(),
                new R4PopulationBasisValidator());

        var measurementPeriod = postLibraryEvaluationPeriodProcessingAndContinuousVariableObservation(
                measureOrNpmResourceHolder, measureDef, periodStart, periodEnd, context);

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
     * Do post-processing after the libraries have been evaluated, such as: setting the measurement period,
     * once again, with the view to running continuousVariableObservation() and computing the
     * interval used in the MeasureReportBuilder.
     * <p/>
     * Now that we've pushed and popped the current library stack, we're doing it again a 3rd time,
     * since this is easier to reason about than leaving duplicate libraries on the stack that
     * through good fortune before we didn't accidentally evaluate twice.
     */
    private Interval postLibraryEvaluationPeriodProcessingAndContinuousVariableObservation(
            MeasureOrNpmResourceHolder measureOrNpmResourceHolder,
            MeasureDef measureDef,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            CqlEngine context) {

        var libraryVersionedIdentifiers = getMultiLibraryIdMeasureEngineDetails(
                        MeasurePlusNpmResourceHolderList.of(measureOrNpmResourceHolder))
                .getLibraryIdentifiers();

        var compiledLibraries = getCompiledLibraries(libraryVersionedIdentifiers, context);

        var libraries =
                compiledLibraries.stream().map(CompiledLibrary::getLibrary).toList();

        // Add back the libraries to the stack, since we popped them off during CQL
        context.getState().init(libraries);

        // Measurement Period: operation parameter defined measurement period
        Interval measurementPeriodParams = buildMeasurementPeriod(periodStart, periodEnd);

        measureProcessorUtils.setMeasurementPeriod(
                measurementPeriodParams,
                context,
                Optional.ofNullable(measureOrNpmResourceHolder.getMeasureUrl())
                        .map(List::of)
                        .orElse(List.of("Unknown Measure URL")));

        // DON'T pop the library off the stack yet, because we need it for continuousVariableObservation()

        // Populate populationDefs that require MeasureDef results
        measureProcessorUtils.continuousVariableObservation(measureDef, context);

        // Now that we've done continuousVariableObservation(), we're safe to pop the libraries off
        // the stack
        popAllLibrariesFromCqlEngine(context, libraries);

        // extract measurement Period from CQL to pass to report Builder
        return measureProcessorUtils.getDefaultMeasurementPeriod(measurementPeriodParams, context);
    }

    public CompositeEvaluationResultsPerMeasure evaluateMeasureWithCqlEngine(
            List<String> subjects,
            Either3<CanonicalType, IdType, Measure> measureEither,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {

        return evaluateMultiMeasuresPlusNpmHoldersWithCqlEngine(
                subjects,
                MeasurePlusNpmResourceHolderList.of(r4MeasureServiceUtils.foldMeasure(measureEither)),
                periodStart,
                periodEnd,
                parameters,
                context);
    }

    // LUEKTODO:  test for coverage
    public CompositeEvaluationResultsPerMeasure evaluateMeasureIdWithCqlEngine(
            List<String> subjects,
            Either3<CanonicalType, IdType, Measure> measureId,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {

        return evaluateMultiMeasuresPlusNpmHoldersWithCqlEngine(
                subjects,
                MeasurePlusNpmResourceHolderList.of(r4MeasureServiceUtils.foldMeasure(measureId)),
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

        return evaluateMultiMeasuresPlusNpmHoldersWithCqlEngine(
                subjects, MeasurePlusNpmResourceHolderList.of(measure), periodStart, periodEnd, parameters, context);
    }

    public CompositeEvaluationResultsPerMeasure evaluateMeasureWithCqlEngine(
            List<String> subjects,
            MeasureOrNpmResourceHolder measureOrNpmResourceHolder,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {

        return evaluateMultiMeasuresPlusNpmHoldersWithCqlEngine(
                subjects,
                MeasurePlusNpmResourceHolderList.of(measureOrNpmResourceHolder),
                periodStart,
                periodEnd,
                parameters,
                context);
    }

    // LUKETODO:  see if we still need this method from cdr-cr
    //    public CompositeEvaluationResultsPerMeasure evaluateMultiMeasureIdsWithCqlEngine(
    //            List<String> subjects,
    //            List<IdType> measureIds,
    //            @Nullable ZonedDateTime periodStart,
    //            @Nullable ZonedDateTime periodEnd,
    //            Parameters parameters,
    //            CqlEngine context) {
    //        return evaluateMultiMeasuresWithCqlEngine(
    //                subjects,
    //                measureIds.stream()
    //                        .map(IIdType::toUnqualifiedVersionless)
    //                        .map(id -> R4MeasureServiceUtils.resolveById(id, repository))
    //                        .toList(),
    //                periodStart,
    //                periodEnd,
    //                parameters,
    //                context);
    //    }

    public CompositeEvaluationResultsPerMeasure evaluateMultiMeasuresWithCqlEngine(
            List<String> subjects,
            List<Measure> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {
        return evaluateMultiMeasuresPlusNpmHoldersWithCqlEngine(
                subjects,
                MeasurePlusNpmResourceHolderList.ofMeasures(measures),
                periodStart,
                periodEnd,
                parameters,
                context);
    }

    // LUKETODO:  who actually calls this besides evaluateMultiMeasuresWithCqlEngine
    CompositeEvaluationResultsPerMeasure evaluateMultiMeasuresPlusNpmHoldersWithCqlEngine(
            List<String> subjects,
            MeasurePlusNpmResourceHolderList measurePlusNpmResourceHolderList,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            Parameters parameters,
            CqlEngine context) {

        measurePlusNpmResourceHolderList.checkMeasureLibraries();

        var measurementPeriodParams = buildMeasurementPeriod(periodStart, periodEnd);
        var zonedMeasurementPeriod = MeasureProcessorUtils.getZonedTimeZoneForEval(
                measureProcessorUtils.getDefaultMeasurementPeriod(measurementPeriodParams, context));

        var measures = measurePlusNpmResourceHolderList.getMeasures();

        // Do this to be backwards compatible with the previous single-library evaluation:
        // Trigger first-pass validation on measure scoring as well as other aspects of the Measures
        R4MeasureDefBuilder.triggerFirstPassValidation(measures);

        // Note that we must build the LibraryEngine BEFORE we call
        // measureProcessorUtils.setMeasurementPeriod(), otherwise, we get an NPE.
        var multiLibraryIdMeasureEngineDetails =
                getMultiLibraryIdMeasureEngineDetails(measurePlusNpmResourceHolderList);

        preLibraryEvaluationPeriodProcessing(
                multiLibraryIdMeasureEngineDetails.getLibraryIdentifiers(),
                measures,
                parameters,
                context,
                measurementPeriodParams);

        // populate results from Library $evaluate
        return measureProcessorUtils.getEvaluationResults(
                subjects, zonedMeasurementPeriod, context, multiLibraryIdMeasureEngineDetails);
    }

    /**
     * Do pre-processing before CQL evaluating the libraries largely centred on setting the correct
     * measurement period and setting the arg parameters for the libraries.
     * <p/>
     * Annoyingly, this involves pushing and popping the libraries off the stack before we do it
     * all over again in the CQL evaluation.
     * It's possible to just push the libraries onto the stack and then let the CQL evaluation
     * evaluate with twice as much libraries in its current stack since only the first set
     * will be popped during evaluation, but this is more difficult to reason about having
     * duplicate libraries on the stack that through good fortune before we didn't accidentally
     * evaluate twice.
     */
    private void preLibraryEvaluationPeriodProcessing(
            List<VersionedIdentifier> libraryVersionedIdentifiers,
            List<Measure> measures,
            Parameters parameters,
            CqlEngine context,
            Interval measurementPeriodParams) {

        var compiledLibraries = getCompiledLibraries(libraryVersionedIdentifiers, context);

        var libraries =
                compiledLibraries.stream().map(CompiledLibrary::getLibrary).toList();

        // We need the libraries on the stack for setMeasurementPeriod(),
        // specifically for .getMeasurementPeriodParameterDef()
        context.getState().init(libraries);

        // if we comment this out MeasureScorerTest and other tests will fail with NPEs
        setArgParameters(parameters, context, compiledLibraries);

        // set measurement Period from CQL if operation parameters are empty
        measureProcessorUtils.setMeasurementPeriod(
                measurementPeriodParams,
                context,
                measures.stream()
                        .map(Measure::getUrl)
                        .map(url -> Optional.ofNullable(url).orElse("Unknown Measure URL"))
                        .toList());

        // Now pop the libraries off the stack, because we'll be adding them back during
        // CQL library evaluation
        popAllLibrariesFromCqlEngine(context, libraries);
    }

    private MultiLibraryIdMeasureEngineDetails getMultiLibraryIdMeasureEngineDetails(
            MeasurePlusNpmResourceHolderList measurePlusNpmResourceHolderList) {

        var libraryIdentifiersToMeasureIds =
                measurePlusNpmResourceHolderList.getMeasuresPlusNpmResourceHolders().stream()
                        .collect(ImmutableListMultimap.toImmutableListMultimap(
                                this::getLibraryVersionIdentifier, // Key function
                                MeasureOrNpmResourceHolder::getMeasureIdElement));

        var libraryEngine = new LibraryEngine(repository, this.measureEvaluationOptions.getEvaluationSettings());

        var builder = MultiLibraryIdMeasureEngineDetails.builder(libraryEngine);

        libraryIdentifiersToMeasureIds
                .entries()
                .forEach(entry -> builder.addLibraryIdToMeasureId(
                        new VersionedIdentifier().withId(entry.getKey().getId()), entry.getValue()));

        return builder.build();
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

    // LUKETODO:  redo javadoc
    /**
     * method to extract Library version defined on the Measure resource
     * @param measureOrNpmResourceHolder resource that has desired Library
     * @return version identifier of Library
     */
    protected VersionedIdentifier getLibraryVersionIdentifier(MeasureOrNpmResourceHolder measureOrNpmResourceHolder) {
        var url = measureOrNpmResourceHolder.getMainLibraryUrl();

        // Check to see if this Library exists in an NPM Package.  If not, search the Repository
        if (!measureOrNpmResourceHolder.hasNpmLibrary()) {
            Bundle b = this.repository.search(Bundle.class, Library.class, Searches.byCanonical(url), null);
            if (b.getEntry().isEmpty()) {
                var errorMsg = "Unable to find Library with url: %s".formatted(url);
                throw new ResourceNotFoundException(errorMsg);
            }
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

    private List<CompiledLibrary> getCompiledLibraries(List<VersionedIdentifier> ids, CqlEngine context) {
        try {
            var resolvedLibraryResults =
                    context.getEnvironment().getLibraryManager().resolveLibraries(ids);

            var allErrors = resolvedLibraryResults.allErrors();
            if (resolvedLibraryResults.hasErrors() || ids.size() > allErrors.size()) {
                return resolvedLibraryResults.allCompiledLibraries();
            }

            if (ids.size() == 1) {
                final List<CqlCompilerException> cqlCompilerExceptions =
                        resolvedLibraryResults.getErrorsFor(ids.get(0));

                if (cqlCompilerExceptions.size() == 1) {
                    throw new IllegalStateException(
                            "Unable to load CQL/ELM for library: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded."
                                    .formatted(ids.get(0).getId()),
                            cqlCompilerExceptions.get(0));
                } else {
                    throw new IllegalStateException(
                            "Unable to load CQL/ELM for library: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded. Errors: %s"
                                    .formatted(
                                            ids.get(0).getId(),
                                            cqlCompilerExceptions.stream()
                                                    .map(CqlCompilerException::getMessage)
                                                    .reduce((s1, s2) -> s1 + "; " + s2)
                                                    .orElse("No error messages found.")));
                }
            }

            throw new IllegalStateException(
                    "Unable to load CQL/ELM for libraries: %s Verify that the Library resource is available in your environment and has CQL/ELM content embedded. Errors: %s"
                            .formatted(ids, allErrors));

        } catch (CqlIncludeException exception) {
            throw new IllegalStateException(
                    "Unable to load CQL/ELM for libraries: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded."
                            .formatted(
                                    ids.stream().map(VersionedIdentifier::getId).toList()),
                    exception);
        }
    }

    // LUKETODO:  merge these two
    protected void checkMeasureLibrary(Measure measure) {
        if (!measure.hasLibrary()) {
            throw new InvalidRequestException(
                    "Measure %s does not have a primary library specified".formatted(measure.getUrl()));
        }
    }

    private void checkMeasureLibrary(MeasureOrNpmResourceHolder measureOrNpmResourceHolder) {
        if (!measureOrNpmResourceHolder.hasLibrary()) {
            throw new InvalidRequestException("Measure %s does not have a primary library specified"
                    .formatted(measureOrNpmResourceHolder.getMeasureUrl()));
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
        setArgParameters(parameters, context, List.of(lib));
    }

    /**
     * Set parameters for included libraries, which may be multiple
     * Note: this may not be the optimal method (e.g. libraries with the same
     * parameter name, but different values)
     * @param parameters CQL parameters passed in from operation
     * @param context CQL engine generated
     */
    protected void setArgParameters(Parameters parameters, CqlEngine context, List<CompiledLibrary> libs) {
        if (parameters != null) {
            Map<String, Object> paramMap = resolveParameterMap(parameters);
            for (CompiledLibrary lib : libs) {
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

    private void popAllLibrariesFromCqlEngine(CqlEngine context, List<org.hl7.elm.r1.Library> libraries) {
        libraries.forEach(lib -> context.getState().exitLibrary(true));
    }

    // LUKETODO:  get rid of this after mining for requirements
    //    private Measure getMeasure(
    //            Either3<CanonicalType, IdType, Measure> measure, NpmResourceInfoForCql npmResourceHolders) {
    //        final Optional<IMeasureAdapter> optMeasure = npmResourceHolders.getMeasure();
    //        if (optMeasure.isPresent() && optMeasure.get().get() instanceof Measure measureFromNpm) {
    //            return measureFromNpm;
    //        }
    //
    //        return measure.fold(this::resolveByUrl, this::resolveById, Function.identity());
    //    }
}

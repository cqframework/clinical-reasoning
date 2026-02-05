package org.opencds.cqf.fhir.cr.measure.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.annotations.VisibleForTesting;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationResultHandler;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorTimeUtils;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MultiLibraryIdMeasureEngineDetails;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@SuppressWarnings("squid:S1135")
public class Dstu3MeasureProcessor {
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final SubjectProvider subjectProvider;
    private final FhirContext fhirContext = FhirContext.forDstu3Cached();
    private final MeasureEvaluationResultHandler measureEvaluationResultHandler;

    public Dstu3MeasureProcessor(IRepository repository, MeasureEvaluationOptions measureEvaluationOptions) {
        this(repository, measureEvaluationOptions, new Dstu3RepositorySubjectProvider());
    }

    public Dstu3MeasureProcessor(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            SubjectProvider subjectProvider) {
        this.repository = Objects.requireNonNull(repository);
        this.measureEvaluationOptions =
                measureEvaluationOptions != null ? measureEvaluationOptions : MeasureEvaluationOptions.defaultOptions();
        this.subjectProvider = subjectProvider;
        this.measureEvaluationResultHandler =
                new MeasureEvaluationResultHandler(this.measureEvaluationOptions, new Dstu3PopulationBasisValidator());
    }

    public MeasureReport evaluateMeasure(
            IdType measureId,
            String periodStart,
            String periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters) {
        var measure = this.repository.read(Measure.class, measureId);
        return this.evaluateMeasure(
                measure, periodStart, periodEnd, reportType, subjectIds, additionalData, parameters);
    }

    // NOTE: Do not make a top-level function that takes a Measure resource. This
    // ensures that
    // the repositories are set up correctly.
    protected MeasureReport evaluateMeasure(
            Measure measure,
            String periodStart,
            String periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters) {

        return evaluateMeasureCaptureDefs(
                        measure, periodStart, periodEnd, reportType, subjectIds, additionalData, parameters)
                .measureReport();
    }

    /**
     * Test-visible evaluation method that captures both MeasureDef and MeasureReport.
     * <p>
     * <strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong>
     * </p>
     * <p>
     * This overload reads the Measure from the repository by ID and delegates to
     * evaluateMeasureCaptureDefs(Measure, ...).
     * </p>
     *
     * @param measureId Measure ID
     * @param periodStart start date string of Measurement Period
     * @param periodEnd end date string of Measurement Period
     * @param reportType type of report
     * @param subjectIds the subjectIds to process
     * @param additionalData additional data bundle
     * @param parameters CQL parameters
     * @return MeasureDefAndDstu3MeasureReport containing both MeasureDef and MeasureReport
     */
    @VisibleForTesting
    MeasureDefAndDstu3MeasureReport evaluateMeasureCaptureDefs(
            IdType measureId,
            String periodStart,
            String periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters) {

        Measure measure = this.repository.read(Measure.class, measureId);
        return evaluateMeasureCaptureDefs(
                measure, periodStart, periodEnd, reportType, subjectIds, additionalData, parameters);
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
     * @param periodStart start date string of Measurement Period
     * @param periodEnd end date string of Measurement Period
     * @param reportType type of report
     * @param subjectIds the subjectIds to process
     * @param additionalData additional data bundle
     * @param parameters CQL parameters
     * @return MeasureDefAndDstu3MeasureReport containing both MeasureDef and MeasureReport
     */
    @VisibleForTesting
    MeasureDefAndDstu3MeasureReport evaluateMeasureCaptureDefs(
            Measure measure,
            String periodStart,
            String periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters) {

        checkMeasureLibrary(measure);

        Interval measurementPeriodParams = MeasureProcessorTimeUtils.buildMeasurementPeriod(periodStart, periodEnd);

        // setup MeasureDef
        var measureDef = new Dstu3MeasureDefBuilder().build(measure);

        var actualRepo = this.repository;
        if (additionalData != null) {
            actualRepo = new FederatedRepository(
                    this.repository, new InMemoryFhirRepository(this.repository.fhirContext(), additionalData));
        }
        var subjects = subjectProvider.getSubjects(actualRepo, subjectIds).toList();
        var evalType = getMeasureEvalType(reportType, subjects);
        var context = Engines.forRepository(
                this.repository, this.measureEvaluationOptions.getEvaluationSettings(), additionalData);

        // Note that we must build the LibraryEngine BEFORE we call
        // measureProcessorUtils.setMeasurementPeriod(), otherwise, we get an NPE.
        var measureLibraryIdEngineDetails = buildLibraryIdEngineDetails(measure, parameters, context);

        // set measurement Period from CQL if operation parameters are empty
        MeasureProcessorTimeUtils.setMeasurementPeriod(
                measurementPeriodParams,
                context,
                Optional.ofNullable(measure.getUrl()).map(List::of).orElse(List.of("Unknown Measure URL")));
        // extract measurement Period from CQL to pass to report Builder
        Interval measurementPeriod =
                MeasureProcessorTimeUtils.getDefaultMeasurementPeriod(measurementPeriodParams, context);
        // set offset of operation parameter measurement period
        ZonedDateTime zonedMeasurementPeriod = MeasureProcessorTimeUtils.getZonedTimeZoneForEval(measurementPeriod);
        // populate results from Library $evaluate
        if (!subjects.isEmpty()) {
            var results = MeasureEvaluationResultHandler.getEvaluationResults(
                    subjectIds, zonedMeasurementPeriod, context, measureLibraryIdEngineDetails);

            // Process Criteria Expression Results
            measureEvaluationResultHandler.processResults(
                    fhirContext, results.processMeasureForSuccessOrFailure(measureDef), measureDef, evalType);
        }

        // Build Measure Report with Results
        MeasureReport measureReport = new Dstu3MeasureReportBuilder()
                .build(measure, measureDef, evalTypeToReportType(evalType), measurementPeriod, subjects);

        return new MeasureDefAndDstu3MeasureReport(measureDef, measureReport);
    }

    // Ideally this would be done in MeasureProcessorUtils, but it's too much work to change for now
    private MultiLibraryIdMeasureEngineDetails buildLibraryIdEngineDetails(
            Measure measure, Parameters parameters, CqlEngine context) {

        var libraryVersionIdentifier = getLibraryVersionIdentifier(measure);

        final LibraryEngine libraryEngine = getLibraryEngine(parameters, libraryVersionIdentifier, context);

        var measureDef = new Dstu3MeasureDefBuilder().build(measure);

        return MultiLibraryIdMeasureEngineDetails.builder(libraryEngine)
                .addLibraryIdToMeasureId(new VersionedIdentifier().withId(libraryVersionIdentifier.getId()), measureDef)
                .build();
    }

    protected MeasureReportType evalTypeToReportType(MeasureEvalType measureEvalType) {
        return switch (measureEvalType) {
            case PATIENT, SUBJECT -> MeasureReportType.INDIVIDUAL;
            case PATIENTLIST, SUBJECTLIST -> MeasureReportType.PATIENTLIST;
            case POPULATION -> MeasureReportType.SUMMARY;
        };
    }

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

        if (parameters != null) {
            Map<String, Object> paramMap = resolveParameterMap(parameters);
            context.getState().setParameters(lib.getLibrary(), paramMap);
            // Set parameters for included libraries
            // Note: this may not be the optimal method (e.g. libraries with the same
            // parameter name, but different
            // values)
            if (lib.getLibrary().getIncludes() != null) {
                lib.getLibrary()
                        .getIncludes()
                        .getDef()
                        .forEach(includeDef -> paramMap.forEach((paramKey, paramValue) -> context.getState()
                                .setParameter(includeDef.getLocalIdentifier(), paramKey, paramValue)));
            }
        }

        return new LibraryEngine(repository, this.measureEvaluationOptions.getEvaluationSettings());
    }

    private VersionedIdentifier getLibraryVersionIdentifier(Measure measure) {
        var reference = measure.getLibrary().get(0);

        var library = this.repository.read(Library.class, reference.getReferenceElement());

        return new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion());
    }

    private MeasureEvalType getMeasureEvalType(String reportType, List<String> subjectIds) {
        return MeasureEvalType.fromCode(reportType)
                .orElse(
                        subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
                                ? MeasureEvalType.POPULATION
                                : MeasureEvalType.SUBJECT);
    }

    private void checkMeasureLibrary(Measure measure) {
        if (!measure.hasLibrary()) {
            throw new InvalidRequestException(
                    "Measure %s does not have a primary library specified".formatted(measure.getUrl()));
        }
    }

    private Map<String, Object> resolveParameterMap(Parameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();
        Dstu3FhirModelResolver modelResolver = new Dstu3FhirModelResolver();
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
}

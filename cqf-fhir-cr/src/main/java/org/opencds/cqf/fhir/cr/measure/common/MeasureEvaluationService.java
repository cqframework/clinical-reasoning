package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

/**
 * Version-agnostic measure evaluation service. This is the single shared implementation of the
 * full evaluation pipeline: environment setup, subject resolution, CQL execution, subject grouping,
 * and per-group scoring.
 *
 * <p>Batch is the primitive — this service always evaluates a {@code List<ResolvedMeasure>}.
 * Single-measure evaluation passes {@code List.of(singleMeasure)}.
 *
 * <p>All version-specific translation happens before this service is called (inbound adapter)
 * and after it returns (outbound adapter). This service never touches FHIR-version-specific types.
 */
public class MeasureEvaluationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MeasureEvaluationService.class);

    private final MeasureEvaluationOptions options;
    private final FhirContext fhirContext;
    private final MeasureEvaluationResultHandler resultHandler;
    private final MeasurePeriodValidator periodValidator;

    public MeasureEvaluationService(
            MeasureEvaluationOptions options,
            FhirContext fhirContext,
            PopulationBasisValidator populationBasisValidator,
            MeasurePeriodValidator periodValidator) {
        this.options = options;
        this.fhirContext = fhirContext;
        this.resultHandler = new MeasureEvaluationResultHandler(options, populationBasisValidator);
        this.periodValidator = periodValidator;
    }

    /**
     * Evaluates one or more measures. This is the full pipeline: period validation, repository
     * setup, subject resolution, CQL execution, subject grouping, and per-group scoring.
     *
     * @param repository      the base repository for resource access
     * @param measures        resolved measures (domain-translated by version-specific inbound adapter)
     * @param request         evaluation request parameters
     * @param environment     endpoint and additional data configuration
     * @param parameters      CQL parameter map (already resolved from version-specific Parameters)
     * @param subjectProvider version-specific subject resolution
     * @return per-measure × per-subject-group scored results plus evaluation context
     */
    public MeasureEvaluationResults evaluate(
            IRepository repository,
            List<ResolvedMeasure> measures,
            MeasureEvaluationRequest request,
            MeasureEnvironment environment,
            Map<String, Object> parameters,
            SubjectProvider subjectProvider) {

        // 1. Period validation
        periodValidator.validatePeriodStartAndEnd(request.periodStart(), request.periodEnd());

        // 2. Repository setup: proxy if all endpoints configured
        var effectiveRepo = resolveRepository(repository, environment);

        // 3. Federate with additional data for subject resolution
        var federatedRepo = federateForAdditionalData(effectiveRepo, environment);

        // 4. Resolve subject, applying practitioner-as-subject override
        var subjectId = resolvePractitionerOverride(request.subjectId(), request.practitioner());

        // 5. Determine evaluation type from request parameters (before subject resolution)
        var subjectIdAsList = subjectId == null ? List.<String>of() : List.of(subjectId);
        var evalType = MeasureEvalType.getEvalType(null, request.reportType(), subjectIdAsList);

        // 6. Resolve subjects from repository
        var subjectRefs = subjectProvider.getSubjects(federatedRepo, subjectId).toList();
        var subjects = subjectRefs.stream().map(SubjectRef::qualified).toList();
        // 7. CQL engine creation
        var additionalData = (IBaseBundle) environment.additionalData();
        CqlEngine context = Engines.forRepository(effectiveRepo, options.getEvaluationSettings(), additionalData);

        // 8. Measurement period
        Interval measurementPeriodParams =
                MeasureProcessorTimeUtils.buildMeasurementPeriod(request.periodStart(), request.periodEnd());

        // 9. Build library engine details for all measures
        var libraryEngine = new LibraryEngine(effectiveRepo, options.getEvaluationSettings());
        var detailsBuilder = MultiLibraryIdMeasureEngineDetails.builder(libraryEngine);
        for (ResolvedMeasure measure : measures) {
            detailsBuilder.addLibraryIdToMeasureId(
                    new VersionedIdentifier().withId(measure.libraryIdentifier().getId()), measure.measureDef());
        }
        var engineDetails = detailsBuilder.build();

        // 10. Inject measurement period into parameters
        var measureUrls = measures.stream()
                .map(m -> Optional.ofNullable(m.url()).orElse("Unknown Measure URL"))
                .toList();
        var paramsMap = new HashMap<>(parameters);
        MeasureProcessorTimeUtils.resolveMeasurementPeriodIntoParameters(
                measurementPeriodParams, context, engineDetails.getLibraryIdentifiers(), measureUrls, paramsMap);

        // 11. Batch CQL execution (once, for all subjects and all measures)
        var zonedMeasurementPeriod = MeasureProcessorTimeUtils.getZonedTimeZoneForEval(
                MeasureProcessorTimeUtils.getDefaultMeasurementPeriod(measurementPeriodParams, context));
        var compositeResults = MeasureEvaluationResultHandler.getEvaluationResults(
                subjectRefs, zonedMeasurementPeriod, context, engineDetails, paramsMap);

        // 12. Score each measure (one score per ResolvedMeasure, covering all subjects)
        var scoredMeasures = measures.stream()
                .map(m -> scoreMeasure(m.measureDef(), compositeResults, evalType, subjects))
                .toList();

        // 13. Resolve measurement period for report
        Interval resolvedPeriod =
                MeasureProcessorTimeUtils.getDefaultMeasurementPeriod(measurementPeriodParams, context);

        return new MeasureEvaluationResults(scoredMeasures, resolvedPeriod, evalType);
    }

    /**
     * If practitioner is set, use it as the evaluation subject (takes precedence over subjectId).
     * Ensures the practitioner reference is qualified with a resource type prefix.
     */
    @Nullable
    private String resolvePractitionerOverride(@Nullable String subjectId, @Nullable String practitioner) {
        if (StringUtils.isNotBlank(practitioner)) {
            var qualifiedPractitioner =
                    practitioner.contains("/") ? practitioner : "Practitioner/".concat(practitioner);
            return qualifiedPractitioner;
        }
        return subjectId;
    }

    private ScoredMeasure scoreMeasure(
            MeasureDef measureDef,
            CompositeEvaluationResultsPerMeasure compositeResults,
            MeasureEvalType evalType,
            List<String> subjects) {
        var outcome = compositeResults.processMeasureForSuccessOrFailure(measureDef);
        var state = resultHandler.processResults(fhirContext, outcome.results(), measureDef, evalType);
        // Route CQL evaluation errors to the per-evaluation state, not the shared MeasureDef
        outcome.errors().forEach(state::addError);
        return new ScoredMeasure(measureDef, state, subjects);
    }

    /**
     * Resolves the effective repository: proxy if all endpoints are configured,
     * otherwise the base repository.
     */
    private IRepository resolveRepository(IRepository base, MeasureEnvironment environment) {
        if (environment.dataEndpoint() != null
                && environment.contentEndpoint() != null
                && environment.terminologyEndpoint() != null) {
            return Repositories.proxy(
                    base,
                    true,
                    environment.dataEndpoint(),
                    environment.contentEndpoint(),
                    environment.terminologyEndpoint());
        }
        // Warn if some but not all endpoints are configured — partial config is likely unintentional
        if (environment.dataEndpoint() != null
                || environment.contentEndpoint() != null
                || environment.terminologyEndpoint() != null) {
            log.warn(
                    "Partial endpoint configuration detected: dataEndpoint={}, contentEndpoint={}, terminologyEndpoint={}. "
                            + "All three endpoints must be configured for proxy repository setup. Falling back to base repository.",
                    environment.dataEndpoint() != null ? "set" : "null",
                    environment.contentEndpoint() != null ? "set" : "null",
                    environment.terminologyEndpoint() != null ? "set" : "null");
        }
        return base;
    }

    /**
     * Federates the base repository with additional data so subjects in the bundle
     * are discoverable during subject resolution.
     */
    private IRepository federateForAdditionalData(IRepository base, MeasureEnvironment environment) {
        if (environment.additionalData() != null) {
            return new FederatedRepository(
                    base, new InMemoryFhirRepository(base.fhirContext(), environment.additionalData()));
        }
        return base;
    }
}

package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEnvironment;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationException;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationResults;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationService;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationState;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureValidationException;
import org.opencds.cqf.fhir.cr.measure.common.ResolvedMeasure;
import org.opencds.cqf.fhir.cr.measure.common.ScoredMeasure;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.builder.BundleBuilder;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.Repositories;

/**
 * R4 measure evaluation orchestrator for both single-measure ({@code $evaluate-measure}) and
 * multi-measure ({@code $evaluate}) operations.
 *
 * <p>This class is the R4 inbound/outbound adapter around {@link MeasureEvaluationService}.
 * It handles version-specific concerns: measure resolution from FHIR R4 resources, R4 parameter
 * conversion, and R4 MeasureReport building from scored results. All domain logic — period
 * validation, subject resolution, CQL execution, scoring — is delegated to the service.</p>
 */
@SuppressWarnings({"squid:S107", "UnstableApiUsage"})
public class R4MultiMeasureService implements R4MeasureEvaluatorSingle, R4MeasureEvaluatorMultiple {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(R4MultiMeasureService.class);

    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final String serverBase;
    private final R4RepositorySubjectProvider subjectProvider;
    private final R4MeasureServiceUtils r4MeasureServiceUtils;
    private final MeasureEvaluationService evaluationService;

    // Used only for measure resolution (building ResolvedMeasure) and parameter conversion.
    // The service owns the CQL engine, repository proxying, and evaluation lifecycle.
    private final R4MeasureProcessor r4MeasureProcessor;

    private enum SingleOrMultiple {
        SINGLE,
        MULTIPLE
    }

    public R4MultiMeasureService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.serverBase = serverBase;
        this.subjectProvider = new R4RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
        this.r4MeasureProcessor = new R4MeasureProcessor(repository, measureEvaluationOptions);
        this.r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        this.evaluationService = new MeasureEvaluationService(
                measureEvaluationOptions,
                FhirContext.forR4Cached(),
                new R4PopulationBasisValidator(),
                measurePeriodValidator);
    }

    // We should eliminate this if/when we eliminate the Measure test class
    public IRepository getRepository() {
        return repository;
    }

    @Override
    public MeasureReport evaluate(
            Either3<CanonicalType, IdType, Measure> measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData,
            Parameters parameters,
            String productLine,
            String practitioner) {

        return evaluateSingleMeasureCaptureDef(
                        measure,
                        periodStart,
                        periodEnd,
                        reportType,
                        subjectId,
                        lastReceivedOn,
                        contentEndpoint,
                        terminologyEndpoint,
                        dataEndpoint,
                        additionalData,
                        parameters,
                        productLine,
                        practitioner)
                .measureReport();
    }

    @VisibleForTesting
    MeasureDefAndR4MeasureReport evaluateSingleMeasureCaptureDef(
            Either3<CanonicalType, IdType, Measure> measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData,
            Parameters parameters,
            String productLine,
            String practitioner) {

        final List<List<MeasureDefAndR4MeasureReport>> resultsAsListOfList = evaluateToListOfList(
                SingleOrMultiple.SINGLE,
                measure,
                List.of(),
                List.of(),
                List.of(),
                periodStart,
                periodEnd,
                reportType,
                subjectId,
                contentEndpoint,
                terminologyEndpoint,
                dataEndpoint,
                additionalData,
                parameters,
                productLine,
                null,
                practitioner);

        if (resultsAsListOfList.size() != 1) {
            throw new MeasureEvaluationException(
                    "Expected only a single MeasureReport but got multiples for measureId: %s and subjectId: %s"
                            .formatted(measure, subjectId));
        }

        final List<MeasureDefAndR4MeasureReport> measureDefAndR4MeasureReports = resultsAsListOfList.get(0);

        if (measureDefAndR4MeasureReports.size() != 1) {
            throw new MeasureEvaluationException(
                    "Expected only a single MeasureReport but got multiples for measureId: %s and subjectId: %s"
                            .formatted(measure, subjectId));
        }

        return measureDefAndR4MeasureReports.get(0);
    }

    @Override
    public Parameters evaluate(
            List<IdType> measureId,
            List<String> measureUrl,
            List<String> measureIdentifier,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subject,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData,
            Parameters parameters,
            String productLine,
            String reporter) {

        return evaluateWithDefs(
                        measureId,
                        measureUrl,
                        measureIdentifier,
                        periodStart,
                        periodEnd,
                        reportType,
                        subject,
                        contentEndpoint,
                        terminologyEndpoint,
                        dataEndpoint,
                        additionalData,
                        parameters,
                        productLine,
                        reporter)
                .parameters();
    }

    @VisibleForTesting
    MeasureDefAndR4ParametersWithMeasureReports evaluateWithDefs(
            List<IdType> measureId,
            List<String> measureUrl,
            List<String> measureIdentifier,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subject,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData,
            Parameters parameters,
            String productLine,
            String reporter) {

        return toMeasureDefAndParametersResults(evaluateToListOfList(
                SingleOrMultiple.MULTIPLE,
                null,
                measureId,
                measureUrl,
                measureIdentifier,
                periodStart,
                periodEnd,
                reportType,
                subject,
                contentEndpoint,
                terminologyEndpoint,
                dataEndpoint,
                additionalData,
                parameters,
                productLine,
                reporter,
                null));
    }

    /**
     * Core evaluation pipeline: resolves measures, delegates to {@link MeasureEvaluationService},
     * and builds version-specific reports from the results.
     */
    private List<List<MeasureDefAndR4MeasureReport>> evaluateToListOfList(
            SingleOrMultiple singleOrMultiple,
            @Nullable Either3<CanonicalType, IdType, Measure> measure,
            List<IdType> measureId,
            List<String> measureUrl,
            List<String> measureIdentifier,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subject,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData,
            Parameters parameters,
            String productLine,
            String reporter,
            @Nullable String practitioner) {

        // ── Version-specific: validate R4 report type values ──
        if (reportType != null) {
            R4MeasureEvalType.fromCode(reportType);
        }

        // ── Version-specific: ensure search parameters ──
        if (measureEvaluationOptions.isEnsureSearchParameters()) {
            r4MeasureServiceUtils.ensureSupplementalDataElementSearchParameter();
        }

        // ── Version-specific: resolve measures ──
        // If custom endpoints are configured, resolve measures from a proxied repository
        // so that content-server-hosted measures and libraries are discoverable.
        final R4MeasureProcessor processorForResolution;
        final R4MeasureServiceUtils utilsForResolution;
        if (dataEndpoint != null && contentEndpoint != null && terminologyEndpoint != null) {
            var proxiedRepo = Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);
            processorForResolution = new R4MeasureProcessor(proxiedRepo, measureEvaluationOptions);
            utilsForResolution = new R4MeasureServiceUtils(proxiedRepo);
        } else {
            processorForResolution = r4MeasureProcessor;
            utilsForResolution = r4MeasureServiceUtils;
        }

        List<Measure> measures = getMeasures(measure, measureId, measureUrl, measureIdentifier, utilsForResolution);

        log.debug("multi-evaluate-measure, measures to evaluate: {}", measures.size());

        // Build version-agnostic ResolvedMeasures. Keep the original Measure for report building.
        var defToMeasure = new LinkedHashMap<MeasureDef, Measure>();
        var resolvedMeasures = new ArrayList<ResolvedMeasure>();
        for (var m : measures) {
            var resolved = processorForResolution.buildResolvedMeasure(m);
            resolvedMeasures.add(resolved);
            defToMeasure.put(resolved.measureDef(), m);
        }

        // ── Version-specific: convert parameters ──
        var params = processorForResolution.resolveParameterMap(parameters);

        // ── Build domain request and environment ──
        var request = new MeasureEvaluationRequest(
                periodStart,
                periodEnd,
                reportType,
                subject,
                singleOrMultiple == SingleOrMultiple.SINGLE ? practitioner : null,
                null,
                productLine);

        var environment = new MeasureEnvironment(contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData);

        // ── Delegate to version-agnostic service ──
        var results =
                evaluationService.evaluate(repository, resolvedMeasures, request, environment, params, subjectProvider);

        // ── Version-specific: build reports from results ──
        return buildReportsFromResults(
                results, defToMeasure, singleOrMultiple, utilsForResolution, reporter, subject, productLine);
    }

    /**
     * Converts version-agnostic {@link MeasureEvaluationResults} into the nested report structure
     * expected by callers.
     *
     * <p>For POPULATION/SUBJECTLIST or single-measure: one list containing one report per measure.
     * For SUBJECT multi-measure: one list per measure, each containing one report per subject.</p>
     */
    private List<List<MeasureDefAndR4MeasureReport>> buildReportsFromResults(
            MeasureEvaluationResults results,
            Map<MeasureDef, Measure> defToMeasure,
            SingleOrMultiple singleOrMultiple,
            R4MeasureServiceUtils serviceUtils,
            String reporter,
            String subjectParam,
            String productLine) {

        var evalType = results.evalType();
        var measurementPeriod = results.measurementPeriod();

        // Per-subject splitting only for SUBJECT eval in the multi-measure path
        boolean perSubject = evalType == MeasureEvalType.SUBJECT && singleOrMultiple == SingleOrMultiple.MULTIPLE;

        List<List<MeasureDefAndR4MeasureReport>> allResults = new ArrayList<>();

        if (!perSubject) {
            // POPULATION, SUBJECTLIST, or SINGLE: one report per measure, all subjects
            var resultList = new ArrayList<MeasureDefAndR4MeasureReport>();
            allResults.add(resultList);

            for (var scored : results.scoredMeasures()) {
                var fhirMeasure = defToMeasure.get(scored.measureDef());
                var report = buildMeasureReport(scored, fhirMeasure, evalType, measurementPeriod, scored.subjects());

                // Post-process: add subject reference for non-individual report types
                serviceUtils.addSubjectReference(report, null, subjectParam);

                applyReporter(serviceUtils, report, reporter);
                initializeReport(report);

                resultList.add(new MeasureDefAndR4MeasureReport(scored.measureDef(), scored.state(), report));
            }
        } else {
            // SUBJECT multi-measure: one list per measure, each containing per-subject reports
            for (var scored : results.scoredMeasures()) {
                var resultList = new ArrayList<MeasureDefAndR4MeasureReport>();
                allResults.add(resultList);

                var fhirMeasure = defToMeasure.get(scored.measureDef());
                for (var subjectId : scored.subjects()) {
                    var report =
                            buildMeasureReport(scored, fhirMeasure, evalType, measurementPeriod, List.of(subjectId));

                    // Post-process: add product line extension for per-subject reports
                    serviceUtils.addProductLineExtension(report, productLine);

                    applyReporter(serviceUtils, report, reporter);
                    initializeReport(report);

                    resultList.add(new MeasureDefAndR4MeasureReport(scored.measureDef(), scored.state(), report));
                }
            }
        }

        return allResults;
    }

    private MeasureReport buildMeasureReport(
            ScoredMeasure scored,
            Measure fhirMeasure,
            MeasureEvalType evalType,
            org.opencds.cqf.cql.engine.runtime.Interval measurementPeriod,
            List<String> subjects) {
        return new R4MeasureReportBuilder()
                .build(
                        fhirMeasure,
                        scored.measureDef(),
                        scored.state(),
                        toReportType(evalType, fhirMeasure),
                        measurementPeriod,
                        subjects);
    }

    private static MeasureReportType toReportType(MeasureEvalType evalType, Measure measure) {
        return switch (evalType) {
            case SUBJECT -> MeasureReportType.INDIVIDUAL;
            case SUBJECTLIST -> MeasureReportType.SUBJECTLIST;
            case POPULATION -> MeasureReportType.SUMMARY;
            default ->
                throw new MeasureValidationException("Unsupported MeasureEvalType: %s for Measure: %s"
                        .formatted(evalType.toCode(), measure.getUrl()));
        };
    }

    private static void applyReporter(R4MeasureServiceUtils serviceUtils, MeasureReport report, String reporter) {
        if (reporter != null && !reporter.isEmpty()) {
            serviceUtils.getReporter(reporter).ifPresent(report::setReporter);
        }
    }

    // ── Measure resolution helpers (version-specific) ──

    private List<Measure> getMeasures(
            @Nullable Either3<CanonicalType, IdType, Measure> measureEither,
            List<IdType> measureId,
            List<String> measureUrl,
            List<String> measureIdentifier,
            R4MeasureServiceUtils serviceUtils) {
        return Optional.ofNullable(measureEither)
                .map(nonNullMeasureEither ->
                        List.of(R4MeasureServiceUtils.foldMeasure(nonNullMeasureEither, this.repository)))
                .orElse(serviceUtils.getMeasures(measureId, measureIdentifier, measureUrl));
    }

    // ── Report packaging helpers ──

    protected void initializeReport(MeasureReport measureReport) {
        if (Strings.isNullOrEmpty(measureReport.getId())) {
            IIdType id = Ids.newId(MeasureReport.class, UUID.randomUUID().toString());
            measureReport.setId(id);
        }
    }

    protected Bundle.BundleEntryComponent getBundleEntry(String serverBase, Resource resource) {
        return new Bundle.BundleEntryComponent().setResource(resource).setFullUrl(getFullUrl(serverBase, resource));
    }

    private MeasureDefAndR4ParametersWithMeasureReports toMeasureDefAndParametersResults(
            List<List<MeasureDefAndR4MeasureReport>> results) {

        final List<MeasureDef> measureDefs = new ArrayList<>();
        final List<MeasureEvaluationState> states = new ArrayList<>();
        final Parameters outputParameters = new Parameters();
        final Map<String, Bundle> bundleBySubject = new HashMap<>();

        for (List<MeasureDefAndR4MeasureReport> result : results) {
            for (MeasureDefAndR4MeasureReport entry : result) {
                measureDefs.add(entry.measureDef());
                states.add(entry.state());

                final MeasureReport measureReport = entry.measureReport();
                final String subject = measureReport.getSubject().getReference();

                Bundle bundle = bundleBySubject.computeIfAbsent(subject, key -> {
                    var newBundle = new BundleBuilder<>(Bundle.class)
                            .withType(BundleType.SEARCHSET.toString())
                            .build();
                    outputParameters.addParameter().setName("return").setResource(newBundle);
                    return newBundle;
                });

                bundle.addEntry(getBundleEntry(serverBase, measureReport));
            }
        }

        return new MeasureDefAndR4ParametersWithMeasureReports(measureDefs, states, outputParameters);
    }

    @Nonnull
    protected List<String> getSubjects(R4RepositorySubjectProvider subjectProvider, String subjectId) {
        return subjectProvider.getSubjects(repository, subjectId).toList();
    }
}

package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_PRODUCT_LINE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_LOCATION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_ORGANIZATION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_PRACTITIONER;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_PRACTITIONER_ROLE;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
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
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;
import org.opencds.cqf.fhir.cr.measure.common.RepositorySubjectProvider;
import org.opencds.cqf.fhir.cr.measure.common.ResolvedMeasure;
import org.opencds.cqf.fhir.cr.measure.common.ScoredMeasure;
import org.opencds.cqf.fhir.cr.measure.common.SubjectRef;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.builder.BundleBuilder;
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
    private final RepositorySubjectProvider subjectProvider;
    private final R4MeasureServiceUtils r4MeasureServiceUtils;
    private final MeasureEvaluationService evaluationService;

    private final R4MeasureResolver r4MeasureResolver;

    public R4MultiMeasureService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.serverBase = serverBase;
        this.subjectProvider = new RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
        this.r4MeasureResolver = new R4MeasureResolver(repository);
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
            IdType measureId,
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
                        measureId,
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
            IdType measureId,
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

        // Resolve the Measure from the IdType before entering the shared pipeline
        final Measure measure = repository.read(Measure.class, measureId);

        final List<List<MeasureDefAndR4MeasureReport>> resultsAsListOfList = evaluateToListOfList(
                false,
                measure,
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
                            .formatted(measureId, subjectId));
        }

        final List<MeasureDefAndR4MeasureReport> measureDefAndR4MeasureReports = resultsAsListOfList.get(0);

        if (measureDefAndR4MeasureReports.size() != 1) {
            throw new MeasureEvaluationException(
                    "Expected only a single MeasureReport but got multiples for measureId: %s and subjectId: %s"
                            .formatted(measureId, subjectId));
        }

        return measureDefAndR4MeasureReports.get(0);
    }

    @Override
    public Parameters evaluate(
            List<MeasureReference> measures,
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
                        measures,
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
            List<MeasureReference> measures,
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
                true,
                null,
                measures,
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
            boolean isMultiMeasureOperation,
            @Nullable Measure measure,
            List<MeasureReference> measureRefs,
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
            var evalType = MeasureEvalType.fromCode(reportType);
            if (evalType.isPresent()
                    && (evalType.get() == MeasureEvalType.PATIENT || evalType.get() == MeasureEvalType.PATIENTLIST)) {
                throw new UnsupportedOperationException(
                        "ReportType: %s, is not an accepted R4 EvalType value.".formatted(reportType));
            }
        }

        // ── Version-specific: resolve measures ──
        // If custom endpoints are configured, resolve measures from a proxied repository
        // so that content-server-hosted measures and libraries are discoverable.
        final R4MeasureResolver resolverForResolution;
        final R4MeasureServiceUtils utilsForResolution;
        if (dataEndpoint != null && contentEndpoint != null && terminologyEndpoint != null) {
            var proxiedRepo = Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);
            resolverForResolution = new R4MeasureResolver(proxiedRepo);
            utilsForResolution = new R4MeasureServiceUtils(proxiedRepo);
        } else {
            resolverForResolution = r4MeasureResolver;
            utilsForResolution = r4MeasureServiceUtils;
        }

        List<Measure> resolvedMeasureList = getMeasures(measure, measureRefs, utilsForResolution);

        log.debug("multi-evaluate-measure, measures to evaluate: {}", resolvedMeasureList.size());

        // Build version-agnostic ResolvedMeasures.
        var resolvedMeasures = new ArrayList<ResolvedMeasure>();
        for (var m : resolvedMeasureList) {
            var resolved = resolverForResolution.buildResolvedMeasure(m);
            resolvedMeasures.add(resolved);
        }

        // ── Version-specific: convert parameters ──
        var params = resolverForResolution.resolveParameterMap(parameters);

        // ── Build domain request and environment ──
        var request = new MeasureEvaluationRequest(
                periodStart,
                periodEnd,
                reportType,
                subject,
                !isMultiMeasureOperation ? practitioner : null,
                null,
                productLine);

        var environment = new MeasureEnvironment(contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData);

        // ── Delegate to version-agnostic service ──
        var results =
                evaluationService.evaluate(repository, resolvedMeasures, request, environment, params, subjectProvider);

        // ── Version-specific: build reports from results ──
        return buildReportsFromResults(results, isMultiMeasureOperation, reporter, subject, productLine);
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
            boolean isMultiMeasureOperation,
            String reporter,
            String subjectParam,
            String productLine) {

        var evalType = results.evalType();
        var measurementPeriod = results.measurementPeriod();

        // Per-subject splitting only for SUBJECT eval in the multi-measure path
        boolean perSubject = evalType == MeasureEvalType.SUBJECT && isMultiMeasureOperation;

        List<List<MeasureDefAndR4MeasureReport>> allResults = new ArrayList<>();

        if (!perSubject) {
            // POPULATION, SUBJECTLIST, or SINGLE: one report per measure, all subjects
            var resultList = new ArrayList<MeasureDefAndR4MeasureReport>();
            allResults.add(resultList);

            for (var scored : results.scoredMeasures()) {
                var report = buildMeasureReport(scored, evalType, measurementPeriod, scored.subjects());

                // Post-process: add subject reference for non-individual report types
                addSubjectReference(report, null, subjectParam);

                applyReporter(report, reporter);
                initializeReport(report);

                resultList.add(new MeasureDefAndR4MeasureReport(scored.measureDef(), scored.state(), report));
            }
        } else {
            // SUBJECT multi-measure: one list per measure, each containing per-subject reports
            for (var scored : results.scoredMeasures()) {
                var resultList = new ArrayList<MeasureDefAndR4MeasureReport>();
                allResults.add(resultList);

                for (var subjectId : scored.subjects()) {
                    var report = buildMeasureReport(scored, evalType, measurementPeriod, List.of(subjectId));

                    // Post-process: add product line extension for per-subject reports
                    addProductLineExtension(report, productLine);

                    applyReporter(report, reporter);
                    initializeReport(report);

                    resultList.add(new MeasureDefAndR4MeasureReport(scored.measureDef(), scored.state(), report));
                }
            }
        }

        return allResults;
    }

    private MeasureReport buildMeasureReport(
            ScoredMeasure scored,
            MeasureEvalType evalType,
            org.opencds.cqf.cql.engine.runtime.Interval measurementPeriod,
            List<String> subjects) {
        return new R4MeasureReportBuilder()
                .build(
                        scored.measureDef(),
                        scored.state(),
                        r4MeasureResolver.evalTypeToReportType(
                                evalType, scored.measureDef().url()),
                        measurementPeriod,
                        subjects);
    }

    // ── Report post-processing (inlined from R4MeasureServiceUtils — single call-site) ──

    private static void applyReporter(MeasureReport report, String reporter) {
        if (reporter != null && !reporter.isEmpty()) {
            resolveReporter(reporter).ifPresent(report::setReporter);
        }
    }

    private static Optional<Reference> resolveReporter(String reporter) {
        if (!reporter.contains("/")) {
            throw new IllegalArgumentException(
                    "R4MultiMeasureService requires '[ResourceType]/[ResourceId]' format to set MeasureReport.reporter reference.");
        }
        Reference reference;
        if (reporter.startsWith(RESOURCE_TYPE_PRACTITIONER_ROLE)) {
            reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_PRACTITIONER_ROLE));
        } else if (reporter.startsWith(RESOURCE_TYPE_PRACTITIONER)) {
            reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_PRACTITIONER));
        } else if (reporter.startsWith(RESOURCE_TYPE_ORGANIZATION)) {
            reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_ORGANIZATION));
        } else if (reporter.startsWith(RESOURCE_TYPE_LOCATION)) {
            reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_LOCATION));
        } else {
            throw new IllegalArgumentException("MeasureReport.reporter does not accept ResourceType: " + reporter);
        }
        return Optional.of(reference);
    }

    private static void addSubjectReference(MeasureReport measureReport, String practitioner, String subjectId) {
        if ((StringUtils.isNotBlank(practitioner) || StringUtils.isNotBlank(subjectId))
                && (measureReport.getType().name().equals("SUMMARY")
                        || measureReport.getType().name().equals("SUBJECTLIST"))) {
            if (StringUtils.isNotBlank(practitioner)) {
                if (!practitioner.contains("/")) {
                    practitioner = "Practitioner/".concat(practitioner);
                }
                measureReport.setSubject(new Reference(practitioner));
            } else {
                if (!subjectId.contains("/")) {
                    subjectId = "Patient/".concat(subjectId);
                }
                measureReport.setSubject(new Reference(subjectId));
            }
        }
    }

    private static void addProductLineExtension(MeasureReport measureReport, String productLine) {
        if (productLine != null) {
            Extension ext = new Extension();
            ext.setUrl(MEASUREREPORT_PRODUCT_LINE_EXT_URL);
            ext.setValue(new StringType(productLine));
            measureReport.addExtension(ext);
        }
    }

    // ── Measure resolution helpers (version-specific) ──

    private List<Measure> getMeasures(
            @Nullable Measure measure, List<MeasureReference> measureRefs, R4MeasureServiceUtils serviceUtils) {
        return Optional.ofNullable(measure).map(List::of).orElse(serviceUtils.getMeasures(measureRefs));
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
    protected List<String> getSubjects(RepositorySubjectProvider subjectProvider, String subjectId) {
        return subjectProvider
                .getSubjects(repository, subjectId)
                .map(SubjectRef::qualified)
                .toList();
    }
}

package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_LOCATION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_ORGANIZATION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_PRACTITIONER;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_PRACTITIONER_ROLE;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEnvironment;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationService;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;
import org.opencds.cqf.fhir.cr.measure.common.RepositorySubjectProvider;
import org.opencds.cqf.fhir.cr.measure.common.ResolvedMeasure;
import org.opencds.cqf.fhir.cr.measure.common.ScoredMeasure;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
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
public class R4MeasureService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(R4MeasureService.class);

    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final String serverBase;
    private final RepositorySubjectProvider subjectProvider;
    private final R4MeasureServiceUtils r4MeasureServiceUtils;
    private final MeasureEvaluationService evaluationService;

    private final R4MeasureResolver r4MeasureResolver;

    public R4MeasureService(
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

    // TODO: eliminate this when we eliminate the Measure test class
    public IRepository getRepository() {
        return repository;
    }

    public String getServerBase() {
        return serverBase;
    }

    // Package-private: test helpers (same package) need these to capture MeasureDef
    R4MeasureResolver resolver() {
        return r4MeasureResolver;
    }

    MeasureEvaluationService evaluationService() {
        return evaluationService;
    }

    RepositorySubjectProvider subjectProvider() {
        return subjectProvider;
    }

    /**
     * Unified evaluate: takes a list of measure references, returns a flat list of MeasureReports.
     *
     * <p>For SUBJECT eval type: one report per measure per subject.
     * For POPULATION/SUBJECTLIST/SUMMARY: one report per measure with all subjects.</p>
     */
    public List<MeasureReport> evaluate(
            List<MeasureReference> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            @Nullable String reportType,
            @Nullable String subject,
            @Nullable String practitioner,
            @Nullable Endpoint contentEndpoint,
            @Nullable Endpoint terminologyEndpoint,
            @Nullable Endpoint dataEndpoint,
            @Nullable Bundle additionalData,
            @Nullable Parameters parameters) {

        // Version-specific: validate R4 report type values
        if (reportType != null) {
            var evalType = MeasureEvalType.fromCode(reportType);
            if (evalType.isPresent()
                    && (evalType.get() == MeasureEvalType.PATIENT || evalType.get() == MeasureEvalType.PATIENTLIST)) {
                throw new UnsupportedOperationException(
                        "ReportType: %s, is not an accepted R4 EvalType value.".formatted(reportType));
            }
        }

        // Version-specific: resolve measures
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

        List<Measure> resolvedMeasureList = utilsForResolution.getMeasures(measures);

        log.debug("evaluate-measure, measures to evaluate: {}", resolvedMeasureList.size());

        // Build version-agnostic ResolvedMeasures
        var resolvedMeasures = new ArrayList<ResolvedMeasure>();
        for (var m : resolvedMeasureList) {
            var resolved = resolverForResolution.buildResolvedMeasure(m);
            resolvedMeasures.add(resolved);
        }

        // Version-specific: convert parameters
        var params = resolverForResolution.resolveParameterMap(parameters);

        // Build domain request and environment
        var request =
                new MeasureEvaluationRequest(periodStart, periodEnd, reportType, subject, practitioner, null, null);

        var environment = new MeasureEnvironment(contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData);

        // Delegate to version-agnostic service
        var results =
                evaluationService.evaluate(repository, resolvedMeasures, request, environment, params, subjectProvider);

        // Version-specific: build reports from results
        var reports = new ArrayList<MeasureReport>();
        var evalType = results.evalType();
        var period = results.measurementPeriod();

        if (evalType == MeasureEvalType.SUBJECT) {
            // Individual mode: one report per measure per subject
            for (var scored : results.scoredMeasures()) {
                for (var subjectId : scored.subjects()) {
                    reports.add(buildMeasureReport(scored, evalType, period, List.of(subjectId)));
                }
            }
        } else {
            // Aggregate mode: one report per measure, all subjects
            for (var scored : results.scoredMeasures()) {
                var report = buildMeasureReport(scored, evalType, period, scored.subjects());
                addSubjectReference(report, subject);
                reports.add(report);
            }
        }

        for (var report : reports) {
            initializeReport(report);
        }

        return reports;
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

    // Report post-processing helpers

    public static void addSubjectReference(MeasureReport measureReport, @Nullable String subjectId) {
        if (StringUtils.isNotBlank(subjectId)
                && (measureReport.getType().name().equals("SUMMARY")
                        || measureReport.getType().name().equals("SUBJECTLIST"))) {
            if (!subjectId.contains("/")) {
                subjectId = "Patient/".concat(subjectId);
            }
            measureReport.setSubject(new Reference(subjectId));
        }
    }

    public static void applyReporter(MeasureReport report, @Nullable String reporter) {
        if (reporter != null && !reporter.isEmpty()) {
            resolveReporter(reporter).ifPresent(report::setReporter);
        }
    }

    private static Optional<Reference> resolveReporter(String reporter) {
        if (!reporter.contains("/")) {
            throw new IllegalArgumentException(
                    "R4MeasureService requires '[ResourceType]/[ResourceId]' format to set MeasureReport.reporter reference.");
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

    public static void addProductLineExtension(MeasureReport measureReport, @Nullable String productLine) {
        if (productLine != null) {
            var ext = new org.hl7.fhir.r4.model.Extension();
            ext.setUrl(
                    org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_PRODUCT_LINE_EXT_URL);
            ext.setValue(new org.hl7.fhir.r4.model.StringType(productLine));
            measureReport.addExtension(ext);
        }
    }

    void initializeReport(MeasureReport measureReport) {
        if (Strings.isNullOrEmpty(measureReport.getId())) {
            IIdType id = Ids.newId(MeasureReport.class, UUID.randomUUID().toString());
            measureReport.setId(id);
        }
    }
}

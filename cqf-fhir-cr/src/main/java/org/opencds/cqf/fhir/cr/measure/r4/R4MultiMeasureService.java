package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;

import ca.uhn.fhir.repository.IRepository;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.CompositeEvaluationResultsPerMeasure;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.builder.BundleBuilder;
import org.opencds.cqf.fhir.utility.repository.Repositories;

/**
 * Alternate MeasureService call to Process MeasureEvaluation for the selected population of subjects against n-number
 * of measure resources. The output of this operation would be a bundle of MeasureReports instead of MeasureReport.
 */
@SuppressWarnings({"squid:S107", "UnstableApiUsage"})
public class R4MultiMeasureService implements R4MeasureEvaluatorMultiple {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(R4MultiMeasureService.class);

    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasurePeriodValidator measurePeriodValidator;
    private final MeasureProcessorUtils measureProcessorUtils = new MeasureProcessorUtils();
    private final String serverBase;
    private final R4RepositorySubjectProvider subjectProvider;
    private final R4MeasureProcessor r4MeasureProcessorStandardRepository;
    private final R4MeasureServiceUtils r4MeasureServiceUtilsStandardRepository;

    public R4MultiMeasureService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.measurePeriodValidator = measurePeriodValidator;
        this.serverBase = serverBase;
        this.subjectProvider = new R4RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
        this.r4MeasureProcessorStandardRepository =
                new R4MeasureProcessor(repository, this.measureEvaluationOptions, this.measureProcessorUtils);
        this.r4MeasureServiceUtilsStandardRepository = new R4MeasureServiceUtils(repository);
    }

    @Override
    public Parameters evaluate(
            List<IdType> measureId,
            List<String> measureUrl,
            List<String> measureIdentifier,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subject, // practitioner passed in here
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

    /**
     * Test-visible evaluation method that captures both MeasureDef and MeasureReport for each measure.
     * <p>
     * <strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong>
     * </p>
     * <p>
     * This method is package-private and annotated with @VisibleForTesting to support
     * test frameworks that need to assert on both pre-scoring state (MeasureDef) and
     * post-scoring state (MeasureReport) for each evaluated measure.
     * </p>
     *
     * @param measureId list of Measure IDs
     * @param measureUrl list of Measure URLs
     * @param measureIdentifier list of Measure identifiers
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report
     * @param subject the subject ID (or practitioner)
     * @param contentEndpoint content endpoint
     * @param terminologyEndpoint terminology endpoint
     * @param dataEndpoint data endpoint
     * @param additionalData additional data bundle
     * @param parameters CQL parameters
     * @param productLine product line
     * @param reporter reporter ID
     * @return MeasureDefAndR4ParametersWithMeasureReports containing Set of MeasureDefs and Parameters with bundled MeasureReports
     */
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

        measurePeriodValidator.validatePeriodStartAndEnd(periodStart, periodEnd);

        final R4MeasureProcessor r4ProcessorToUse;
        final R4MeasureServiceUtils r4MeasureServiceUtilsToUse;
        if (dataEndpoint != null && contentEndpoint != null && terminologyEndpoint != null) {
            var repositoryToUse =
                    Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);

            r4ProcessorToUse =
                    new R4MeasureProcessor(repositoryToUse, this.measureEvaluationOptions, this.measureProcessorUtils);

            r4MeasureServiceUtilsToUse = new R4MeasureServiceUtils(repositoryToUse);
        } else {
            r4ProcessorToUse = r4MeasureProcessorStandardRepository;
            r4MeasureServiceUtilsToUse = r4MeasureServiceUtilsStandardRepository;
        }

        r4MeasureServiceUtilsToUse.ensureSupplementalDataElementSearchParameter();
        List<Measure> measures = r4MeasureServiceUtilsToUse.getMeasures(measureId, measureIdentifier, measureUrl);
        log.info("multi-evaluate-measure, measures to evaluate: {}", measures.size());

        var evalType = r4MeasureServiceUtilsToUse.getMeasureEvalType(reportType, subject);

        var subjects = getSubjects(subjectProvider, subject);

        var context = Engines.forRepository(
                r4ProcessorToUse.getRepository(),
                this.measureEvaluationOptions.getEvaluationSettings(),
                additionalData);

        var compositeEvaluationResultsPerMeasure = r4ProcessorToUse.evaluateMultiMeasuresWithCqlEngine(
                subjects, measures, periodStart, periodEnd, parameters, context);

        // Build bundles correctly based on evaluation type
        if (evalType.equals(MeasureEvalType.POPULATION) || evalType.equals(MeasureEvalType.SUBJECTLIST)) {
            return populationMeasureReport(
                    r4ProcessorToUse,
                    r4MeasureServiceUtilsToUse,
                    compositeEvaluationResultsPerMeasure,
                    context,
                    measures,
                    periodStart,
                    periodEnd,
                    reportType,
                    evalType,
                    subject,
                    subjects,
                    productLine,
                    reporter);
        } else {
            return subjectMeasureReport(
                    r4ProcessorToUse,
                    r4MeasureServiceUtilsToUse,
                    compositeEvaluationResultsPerMeasure,
                    context,
                    measures,
                    periodStart,
                    periodEnd,
                    reportType,
                    evalType,
                    subjects,
                    productLine,
                    reporter);
        }
    }

    protected MeasureDefAndR4ParametersWithMeasureReports populationMeasureReport(
            R4MeasureProcessor r4Processor,
            R4MeasureServiceUtils r4MeasureServiceUtils,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure,
            CqlEngine context,
            List<Measure> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            String subjectParam,
            List<String> subjects,
            String productLine,
            String reporter) {

        final List<MeasureDef> measureDefs = new ArrayList<>();

        // Create Parameters to hold the bundle(s)
        Parameters result = new Parameters();

        // create bundle - ONE bundle for all measures
        Bundle bundle = new BundleBuilder<>(Bundle.class)
                .withType(BundleType.SEARCHSET.toString())
                .build();

        var totalMeasures = measures.size();
        for (Measure measure : measures) {
            // Capture both MeasureDef and MeasureReport
            var captured = r4Processor.evaluateMeasureCaptureDefs(
                    measure,
                    periodStart,
                    periodEnd,
                    reportType,
                    subjects,
                    evalType,
                    context,
                    compositeEvaluationResultsPerMeasure);

            measureDefs.add(captured.measureDef());

            MeasureReport measureReport = captured.measureReport();

            // add ProductLine after report is generated
            measureReport = r4MeasureServiceUtils.addProductLineExtension(measureReport, productLine);

            // add subject reference for non-individual reportTypes
            measureReport = r4MeasureServiceUtils.addSubjectReference(measureReport, null, subjectParam);

            // add reporter if available
            if (reporter != null && !reporter.isEmpty()) {
                measureReport.setReporter(
                        r4MeasureServiceUtils.getReporter(reporter).orElse(null));
            }
            // add id to measureReport
            initializeReport(measureReport);

            // add report to bundle
            bundle.addEntry(getBundleEntry(serverBase, measureReport));

            // progress feedback
            var measureUrl = measureReport.getMeasure();
            if (!measureUrl.isEmpty()) {
                log.debug(
                        "Completed evaluation for Measure: {}, Measures remaining to evaluate: {}",
                        measureUrl,
                        totalMeasures--);
            }
        }

        // add bundle to result
        result.addParameter().setName("return").setResource(bundle);

        return new MeasureDefAndR4ParametersWithMeasureReports(measureDefs, result);
    }

    protected MeasureDefAndR4ParametersWithMeasureReports subjectMeasureReport(
            R4MeasureProcessor r4Processor,
            R4MeasureServiceUtils r4MeasureServiceUtils,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure,
            CqlEngine context,
            List<Measure> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            List<String> subjects,
            String productLine,
            String reporter) {

        final List<MeasureDef> measureDefs = new ArrayList<>();

        // Create Parameters to hold the bundle(s)
        Parameters result = new Parameters();

        // create individual reports for each subject, and each measure
        // ONE bundle PER SUBJECT containing all their measure reports
        var totalReports = subjects.size() * measures.size();
        var totalMeasures = measures.size();
        var subjectMeasures = new HashMap<String, List<Resource>>();
        subjects.forEach(s -> subjectMeasures.put(s, new ArrayList<>()));
        log.debug(
                "Evaluating individual MeasureReports for {} patients, and {} measures",
                subjects.size(),
                measures.size());
        for (Measure measure : measures) {
            for (String subject : subjects) {
                // Capture both MeasureDef and MeasureReport
                var captured = r4Processor.evaluateMeasureCaptureDefs(
                        measure,
                        periodStart,
                        periodEnd,
                        reportType,
                        Collections.singletonList(subject),
                        evalType,
                        context,
                        compositeEvaluationResultsPerMeasure);

                measureDefs.add(captured.measureDef());

                MeasureReport measureReport = captured.measureReport();

                // add ProductLine after report is generated
                measureReport = r4MeasureServiceUtils.addProductLineExtension(measureReport, productLine);

                // add reporter if available
                if (reporter != null && !reporter.isEmpty()) {
                    measureReport.setReporter(
                            r4MeasureServiceUtils.getReporter(reporter).orElse(null));
                }
                // add id to measureReport
                initializeReport(measureReport);

                // add report to subject list
                subjectMeasures.get(subject).add(measureReport);

                // progress feedback
                var measureUrl = measureReport.getMeasure();
                if (!measureUrl.isEmpty()) {
                    log.debug("MeasureReports remaining to evaluate {}", totalReports--);
                }
            }
            if (measure.hasUrl()) {
                log.info(
                        "Completed evaluation for Measure: {}, Measures remaining to evaluate: {}",
                        measure.getUrl(),
                        totalMeasures--);
            }
        }

        // create subject bundles - ONE per subject
        subjects.forEach(s -> {
            Bundle bundle = new BundleBuilder<>(Bundle.class)
                    .withType(BundleType.SEARCHSET.toString())
                    .build();
            // add subject reports to bundle
            subjectMeasures.get(s).forEach(r -> bundle.addEntry(getBundleEntry(serverBase, r)));
            // add bundle to result
            result.addParameter().setName("return").setResource(bundle);
        });

        return new MeasureDefAndR4ParametersWithMeasureReports(measureDefs, result);
    }

    protected List<String> getSubjects(R4RepositorySubjectProvider subjectProvider, String subjectId) {
        return subjectProvider.getSubjects(repository, subjectId).toList();
    }

    protected void initializeReport(MeasureReport measureReport) {
        if (Strings.isNullOrEmpty(measureReport.getId())) {
            IIdType id = Ids.newId(MeasureReport.class, UUID.randomUUID().toString());
            measureReport.setId(id);
        }
    }

    protected Bundle.BundleEntryComponent getBundleEntry(String serverBase, Resource resource) {
        return new Bundle.BundleEntryComponent().setResource(resource).setFullUrl(getFullUrl(serverBase, resource));
    }
}

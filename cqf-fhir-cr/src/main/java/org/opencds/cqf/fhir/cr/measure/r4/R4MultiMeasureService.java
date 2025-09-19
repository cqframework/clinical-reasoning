package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;

import ca.uhn.fhir.repository.IRepository;
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

        measurePeriodValidator.validatePeriodStartAndEnd(periodStart, periodEnd);

        final R4MeasureProcessor r4ProcessorToUse;
        final R4MeasureServiceUtils r4MeasureServiceUtilsToUse;
        if (dataEndpoint != null && contentEndpoint != null && terminologyEndpoint != null) {
            // if needing to use proxy repository, initialize new R4MeasureProcessor and R4MeasureServiceUtils
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

        // get subjects
        var subjects = getSubjects(subjectProvider, subject);

        // create parameters
        var result = new Parameters();

        var context = Engines.forRepository(
                r4ProcessorToUse.getRepository(),
                this.measureEvaluationOptions.getEvaluationSettings(),
                additionalData);

        // This is basically a Map of measure -> subject -> EvaluationResult
        var compositeEvaluationResultsPerMeasure = r4ProcessorToUse.evaluateMultiMeasuresWithCqlEngine(
                subjects, measures, periodStart, periodEnd, parameters, context);

        // evaluate Measures
        if (evalType.equals(MeasureEvalType.POPULATION) || evalType.equals(MeasureEvalType.SUBJECTLIST)) {
            populationMeasureReport(
                    r4ProcessorToUse,
                    r4MeasureServiceUtilsToUse,
                    compositeEvaluationResultsPerMeasure,
                    context,
                    result,
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
            subjectMeasureReport(
                    r4ProcessorToUse,
                    r4MeasureServiceUtilsToUse,
                    compositeEvaluationResultsPerMeasure,
                    context,
                    result,
                    measures,
                    periodStart,
                    periodEnd,
                    reportType,
                    evalType,
                    subjects,
                    productLine,
                    reporter);
        }

        return result;
    }

    protected void populationMeasureReport(
            R4MeasureProcessor r4Processor,
            R4MeasureServiceUtils r4MeasureServiceUtils,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure,
            CqlEngine context,
            Parameters result,
            List<Measure> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            String subjectParam,
            List<String> subjects,
            String productLine,
            String reporter) {

        // create bundle
        Bundle bundle = new BundleBuilder<>(Bundle.class)
                .withType(BundleType.SEARCHSET.toString())
                .build();

        var totalMeasures = measures.size();
        for (Measure measure : measures) {
            MeasureReport measureReport;
            // evaluate each measure
            measureReport = r4Processor.evaluateMeasure(
                    measure,
                    periodStart,
                    periodEnd,
                    reportType,
                    subjects,
                    evalType,
                    context,
                    compositeEvaluationResultsPerMeasure);

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
    }

    protected void subjectMeasureReport(
            R4MeasureProcessor r4Processor,
            R4MeasureServiceUtils r4MeasureServiceUtils,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure,
            CqlEngine context,
            Parameters result,
            List<Measure> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            List<String> subjects,
            String productLine,
            String reporter) {

        // create individual reports for each subject, and each measure
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
                MeasureReport measureReport;
                // evaluate each measure
                measureReport = r4Processor.evaluateMeasure(
                        measure,
                        periodStart,
                        periodEnd,
                        reportType,
                        Collections.singletonList(subject),
                        evalType,
                        context,
                        compositeEvaluationResultsPerMeasure);

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

        // create subject bundles
        subjects.forEach(s -> {
            Bundle bundle = new BundleBuilder<>(Bundle.class)
                    .withType(BundleType.SEARCHSET.toString())
                    .build();
            // add subject reports to bundle
            subjectMeasures.get(s).forEach(r -> bundle.addEntry(getBundleEntry(serverBase, r)));
            // add bundle to result
            result.addParameter().setName("return").setResource(bundle);
        });
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

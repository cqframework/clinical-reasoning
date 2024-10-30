package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;

import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.builder.BundleBuilder;
import org.opencds.cqf.fhir.utility.repository.Repositories;

/**
 * Alternate MeasureService call to Process MeasureEvaluation for the selected population of subjects against n-number
 * of measure resources. The output of this operation would be a bundle of MeasureReports instead of MeasureReport.
 */
public class R4MultiMeasureService implements R4MeasureEvaluatorMultiple {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(R4MultiMeasureService.class);
    private Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasurePeriodValidator measurePeriodValidator;
    private String serverBase;

    private final R4RepositorySubjectProvider subjectProvider;

    private R4MeasureProcessor r4Processor;

    private R4MeasureServiceUtils r4MeasureServiceUtils;

    public R4MultiMeasureService(
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.measurePeriodValidator = measurePeriodValidator;
        this.serverBase = serverBase;

        subjectProvider = new R4RepositorySubjectProvider();

        r4Processor = new R4MeasureProcessor(repository, this.measureEvaluationOptions, subjectProvider);

        r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
    }

    @Override
    public Bundle evaluate(
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

        log.info("6723: START evaluate: measureIds: {}, subject: {}, reportType: {}", measureId, subject, reportType);

        measurePeriodValidator.validatePeriodStartAndEnd(periodStart, periodEnd);

        if (dataEndpoint != null && contentEndpoint != null && terminologyEndpoint != null) {
            // if needing to use proxy repository, override constructors
            repository = Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);

            r4Processor = new R4MeasureProcessor(repository, this.measureEvaluationOptions, subjectProvider);

            r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        }
        // LUKETODO:  replace this with a constructor dependency to indicate we have the searchparameter or nothing
//        r4MeasureServiceUtils.ensureSupplementalDataElementSearchParameter();
        List<Measure> measures = r4MeasureServiceUtils.getMeasures(measureId, measureIdentifier, measureUrl);
        log.info("multi-evaluate-measure, measures to evaluate: {}", measures.size());

        var evalType = MeasureEvalType.fromCode(reportType)
                .orElse(subject == null || subject.isEmpty() ? MeasureEvalType.POPULATION : MeasureEvalType.SUBJECT);

        // get subjects
        var subjects = getSubjects(subjectProvider, subject, evalType);

        // create bundle
        Bundle bundle = new BundleBuilder<>(Bundle.class)
                .withType(BundleType.SEARCHSET.toString())
                .build();

        // evaluate Measures
        if (evalType.equals(MeasureEvalType.POPULATION) || evalType.equals(MeasureEvalType.SUBJECTLIST)) {
            populationMeasureReport(
                    bundle,
                    measures,
                    periodStart,
                    periodEnd,
                    reportType,
                    evalType,
                    subject,
                    subjects,
                    parameters,
                    additionalData,
                    productLine,
                    reporter);
        } else {
            subjectMeasureReport(
                    bundle,
                    measures,
                    periodStart,
                    periodEnd,
                    reportType,
                    evalType,
                    subjects,
                    parameters,
                    additionalData,
                    productLine,
                    reporter);
        }

        return bundle;
    }

    protected void populationMeasureReport(
            Bundle bundle,
            List<Measure> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            String subjectParam,
            List<String> subjects,
            Parameters parameters,
            Bundle additionalData,
            String productLine,
            String reporter) {

        // one aggregated MeasureReport per Measure
        var totalMeasures = measures.size();
        for (Measure measure : measures) {
            MeasureReport measureReport;
            // evaluate each measure
            measureReport = r4Processor.evaluateMeasure(
                    measure, periodStart, periodEnd, reportType, subjects, additionalData, parameters, evalType);

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
    }

    protected void subjectMeasureReport(
            Bundle bundle,
            List<Measure> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            List<String> subjects,
            Parameters parameters,
            Bundle additionalData,
            String productLine,
            String reporter) {

        // create individual reports for each subject, and each measure
        var totalReports = subjects.size() * measures.size();
        var totalMeasures = measures.size();
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
                        additionalData,
                        parameters,
                        evalType);

                // add ProductLine after report is generated
                measureReport = r4MeasureServiceUtils.addProductLineExtension(measureReport, productLine);

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
    }

    protected List<String> getSubjects(
            R4RepositorySubjectProvider subjectProvider, String subjectId, MeasureEvalType evalType) {

        return subjectProvider.getSubjects(repository, evalType, subjectId).collect(Collectors.toList());
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

package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
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
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
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
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.builder.BundleBuilder;

/**
 * Alternate MeasureService call to Process MeasureEvaluation for the selected population of subjects against n-number
 * of measure resources. The output of this operation would be a bundle of MeasureReports instead of MeasureReport.
 */
@SuppressWarnings({"squid:S107", "UnstableApiUsage"})
public class R4MultiMeasureService implements R4MeasureEvaluatorSingle, R4MeasureEvaluatorMultiple {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(R4MultiMeasureService.class);

    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasurePeriodValidator measurePeriodValidator;
    private final String serverBase;
    private final R4RepositorySubjectProvider subjectProvider;

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
        this.measurePeriodValidator = measurePeriodValidator;
        this.serverBase = serverBase;
        this.subjectProvider = new R4RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
    }

    // We should eliminate this if/when we eliminate the Measure test class
    public IRepository getRepository() {
        return repository;
    }

    @Override
    public MeasureReport evaluate(
            MeasureReference measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
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
                        repository,
                        parameters,
                        productLine,
                        null) // reporter is null in the single measure case
                .measureReport();
    }

    @VisibleForTesting
    MeasureDefAndR4MeasureReport evaluateSingleMeasureCaptureDef(
            MeasureReference measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
            IRepository resolvedRepo,
            Parameters parameters,
            String productLine,
            String practitioner) {

        final List<List<MeasureDefAndR4MeasureReport>> resultsAsListOfList = evaluateToListOfList(
                SingleOrMultiple.SINGLE,
                List.of(measure),
                periodStart,
                periodEnd,
                reportType,
                subjectId,
                resolvedRepo,
                parameters,
                productLine,
                null,
                practitioner); // reporter is null in the single measure case

        if (resultsAsListOfList.size() != 1) {
            throw new InternalErrorException(
                    "Expected only a single MeasureReport but got multiples for measureId: %s and subjectId: %s"
                            .formatted(measure, subjectId));
        }

        final List<MeasureDefAndR4MeasureReport> measureDefAndR4MeasureReports = resultsAsListOfList.get(0);

        if (measureDefAndR4MeasureReports.size() != 1) {
            throw new InternalErrorException(
                    "Expected only a single MeasureReport but got multiples for measureId: %s and subjectId: %s"
                            .formatted(measure, subjectId));
        }

        return measureDefAndR4MeasureReports.get(0);
    }

    @Override
    public Parameters evaluate(
            List<MeasureReference> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subject, // practitioner passed in here
            Parameters parameters,
            String productLine,
            String reporter) {

        return evaluateWithDefs(
                        measures,
                        periodStart,
                        periodEnd,
                        reportType,
                        subject,
                        repository,
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
     * @param measures list of MeasureReferences
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report
     * @param subject the subject ID (or practitioner)
     * @param resolvedRepo fully configured repository (endpoints proxied, data federated)
     * @param parameters CQL parameters
     * @param productLine product line
     * @param reporter reporter ID
     * @return MeasureDefAndR4ParametersWithMeasureReports containing Set of MeasureDefs and Parameters with bundled MeasureReports
     */
    @VisibleForTesting
    MeasureDefAndR4ParametersWithMeasureReports evaluateWithDefs(
            List<MeasureReference> measures,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subject,
            IRepository resolvedRepo,
            Parameters parameters,
            String productLine,
            String reporter) {

        return toMeasureDefAndParametersResults(evaluateToListOfList(
                SingleOrMultiple.MULTIPLE,
                measures,
                periodStart,
                periodEnd,
                reportType,
                subject,
                resolvedRepo,
                parameters,
                productLine,
                reporter,
                null));
    }

    private List<List<MeasureDefAndR4MeasureReport>> evaluateToListOfList(
            SingleOrMultiple singleOrMultiple,
            List<MeasureReference> measureRefs,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subject,
            IRepository resolvedRepo,
            Parameters parameters,
            String productLine,
            String reporter,
            @Nullable String practitioner) {

        measurePeriodValidator.validatePeriodStartAndEnd(periodStart, periodEnd);

        var r4ProcessorToUse = new R4MeasureProcessor(resolvedRepo, this.measureEvaluationOptions);
        var r4MeasureServiceUtilsToUse = new R4MeasureServiceUtils(resolvedRepo);

        if (measureEvaluationOptions.isEnsureSearchParameters()) {
            r4MeasureServiceUtilsToUse.ensureSupplementalDataElementSearchParameter();
        }

        // backward compatibility: in the single measure case, we set the subjectId to practitioner,
        // but we don't in the multi-measure case.
        final String subjectToUse;
        if (SingleOrMultiple.SINGLE == singleOrMultiple) {
            if (StringUtils.isNotBlank(practitioner)) {
                if (!practitioner.contains("/")) {
                    practitioner = "Practitioner/".concat(practitioner);
                }
                subjectToUse = practitioner;
            } else {
                subjectToUse = subject;
            }
        } else {
            subjectToUse = subject;
        }

        final List<Measure> measures = r4MeasureServiceUtilsToUse.getMeasures(measureRefs);

        log.debug("multi-evaluate-measure, measures to evaluate: {}", measures.size());

        var evalType = r4MeasureServiceUtilsToUse.getMeasureEvalType(reportType, subjectToUse);

        var subjects =
                switch (singleOrMultiple) {
                    case SINGLE -> getSubjectsForEvaluateSingle(subjectToUse, resolvedRepo);
                    case MULTIPLE -> getSubjects(subjectProvider, subjectToUse);
                };

        var context = Engines.forRepository(resolvedRepo, this.measureEvaluationOptions.getEvaluationSettings(), null);

        final CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure =
                r4ProcessorToUse.evaluateMultiMeasuresWithCqlEngine(
                        subjects, measures, periodStart, periodEnd, parameters, context);

        if (SingleOrMultiple.SINGLE == singleOrMultiple
                || evalType.equals(MeasureEvalType.POPULATION)
                || evalType.equals(MeasureEvalType.SUBJECTLIST)) {
            return populationOrSingleMeasureReport(
                    r4ProcessorToUse,
                    r4MeasureServiceUtilsToUse,
                    compositeEvaluationResultsPerMeasure,
                    context,
                    measures,
                    subjects,
                    periodStart,
                    periodEnd,
                    reportType,
                    evalType,
                    reporter,
                    subject);
        }

        return subjectReport(
                r4ProcessorToUse,
                r4MeasureServiceUtilsToUse,
                compositeEvaluationResultsPerMeasure,
                context,
                measures,
                subjects,
                periodStart,
                periodEnd,
                reportType,
                evalType,
                reporter,
                productLine);
    }

    private List<List<MeasureDefAndR4MeasureReport>> populationOrSingleMeasureReport(
            R4MeasureProcessor r4Processor,
            R4MeasureServiceUtils r4MeasureServiceUtils,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure,
            CqlEngine context,
            List<Measure> measures,
            List<String> subjects,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            String reporter,
            String subjectParam) {

        final List<List<MeasureDefAndR4MeasureReport>> listOfListOfMeasureEvalResults = evaluateMultiMeasureCaptureDefs(
                r4Processor,
                r4MeasureServiceUtils,
                compositeEvaluationResultsPerMeasure,
                context,
                measures,
                List.of(subjects),
                // add subject reference for non-individual reportTypes
                measureReport -> r4MeasureServiceUtils.addSubjectReference(measureReport, null, subjectParam),
                periodStart,
                periodEnd,
                reportType,
                evalType,
                reporter);

        if (listOfListOfMeasureEvalResults.size() != 1) {
            throw new InternalErrorException("Expected only a single MeasureReport");
        }

        return listOfListOfMeasureEvalResults;
    }

    private List<List<MeasureDefAndR4MeasureReport>> subjectReport(
            R4MeasureProcessor r4Processor,
            R4MeasureServiceUtils r4MeasureServiceUtils,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure,
            CqlEngine context,
            List<Measure> measures,
            List<String> subjects,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            String reporter,
            String productLine) {

        return evaluateMultiMeasureCaptureDefs(
                r4Processor,
                r4MeasureServiceUtils,
                compositeEvaluationResultsPerMeasure,
                context,
                measures,
                subjects.stream().map(List::of).toList(),
                // add ProductLine after report is generated
                measureReport -> r4MeasureServiceUtils.addProductLineExtension(measureReport, productLine),
                periodStart,
                periodEnd,
                reportType,
                evalType,
                reporter);
    }

    private List<List<MeasureDefAndR4MeasureReport>> evaluateMultiMeasureCaptureDefs(
            R4MeasureProcessor r4Processor,
            R4MeasureServiceUtils r4MeasureServiceUtils,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure,
            CqlEngine context,
            List<Measure> measures,
            List<List<String>> subjectGroups,
            Consumer<MeasureReport> measureReportMutator,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            String reporter) {

        // create individual reports for each subject, and each measure
        // ONE bundle PER SUBJECT containing all their measure reports
        var totalReports = subjectGroups.size() * measures.size();
        var totalMeasures = measures.size();

        final List<List<MeasureDefAndR4MeasureReport>> results = new ArrayList<>();

        List<MeasureDefAndR4MeasureReport> result = null;

        if (subjectGroups.size() == 1) {
            result = new ArrayList<>();
            results.add(result);
        }

        for (Measure measure : measures) {

            if (subjectGroups.size() > 1) {
                result = new ArrayList<>();
                results.add(result);
            }

            for (List<String> subjectGroup : subjectGroups) {

                final MeasureDefAndR4MeasureReport measureDefAndR4MeasureReport = evaluateMeasureCaptureDef(
                        r4Processor,
                        r4MeasureServiceUtils,
                        compositeEvaluationResultsPerMeasure,
                        context,
                        measure,
                        periodStart,
                        periodEnd,
                        reportType,
                        evalType,
                        subjectGroup,
                        reporter);

                measureReportMutator.accept(measureDefAndR4MeasureReport.measureReport());

                if (result != null) {
                    result.add(measureDefAndR4MeasureReport);
                }

                // progress feedback
                var measureUrl = measure.getUrl();
                if (StringUtils.isNotBlank(measureUrl)) {
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

        return results;
    }

    private MeasureDefAndR4MeasureReport evaluateMeasureCaptureDef(
            R4MeasureProcessor r4Processor,
            R4MeasureServiceUtils r4MeasureServiceUtils,
            CompositeEvaluationResultsPerMeasure compositeEvaluationResultsPerMeasure,
            CqlEngine context,
            Measure measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            MeasureEvalType evalType,
            List<String> subjects,
            String reporter) {
        // Capture both MeasureDef and MeasureReport
        final MeasureDefAndR4MeasureReport captured = r4Processor.evaluateMeasureCaptureDef(
                measure,
                periodStart,
                periodEnd,
                reportType,
                subjects,
                evalType,
                context,
                compositeEvaluationResultsPerMeasure);

        final MeasureReport measureReport = captured.measureReport();

        // add reporter if available
        if (reporter != null && !reporter.isEmpty()) {
            measureReport.setReporter(
                    r4MeasureServiceUtils.getReporter(reporter).orElse(null));
        }
        // add id to measureReport
        initializeReport(measureReport);

        // The original MeasureDefAndR4MeasureReport contains the MeasureReport we just mutated
        return captured;
    }

    private MeasureDefAndR4ParametersWithMeasureReports toMeasureDefAndParametersResults(
            List<List<MeasureDefAndR4MeasureReport>> results) {

        final List<MeasureDef> measureDefs = new ArrayList<>();

        // Create Parameters to hold the bundle(s)
        final Parameters parameters = new Parameters();

        final Map<String, Bundle> bundleBySubject = new HashMap<>();

        for (List<MeasureDefAndR4MeasureReport> result : results) {

            Bundle bundle;
            for (MeasureDefAndR4MeasureReport measureDefAndR4MeasureReport : result) {
                measureDefs.add(measureDefAndR4MeasureReport.measureDef());

                // add report to bundle
                final MeasureReport measureReport = measureDefAndR4MeasureReport.measureReport();

                final String subject = measureReport.getSubject().getReference();

                if (bundleBySubject.containsKey(subject)) {
                    bundle = bundleBySubject.get(subject);
                } else {
                    bundle = new BundleBuilder<>(Bundle.class)
                            .withType(BundleType.SEARCHSET.toString())
                            .build();
                    bundleBySubject.put(subject, bundle);
                    parameters.addParameter().setName("return").setResource(bundle);
                }

                bundle.addEntry(getBundleEntry(serverBase, measureReport));
            }
        }

        return new MeasureDefAndR4ParametersWithMeasureReports(measureDefs, parameters);
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

    @Nonnull
    private List<String> getSubjectsForEvaluateSingle(String subjectId, IRepository subjectRepo) {
        return subjectProvider
                .getSubjects(
                        subjectRepo,
                        Optional.ofNullable(subjectId).map(List::of).orElse(List.of()))
                .toList();
    }
}

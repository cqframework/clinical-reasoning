package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationParameters;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;
import org.opencds.cqf.fhir.cr.measure.common.MeasureSubject;
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
    public MeasureReport evaluate(MeasureReference measure, MeasureEvaluationRequest request, Parameters parameters) {
        return evaluateSingleMeasureCaptureDef(measure, request, repository, parameters)
                .measureReport();
    }

    /**
     * Test-visible evaluation method that captures both MeasureDef and MeasureReport.
     * <p>
     * <strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong>
     * </p>
     *
     * @param measure      the measure to evaluate
     * @param request      evaluation request parameters
     * @param resolvedRepo fully configured repository (endpoints proxied, data federated)
     * @param parameters   CQL parameters
     * @return MeasureDefAndR4MeasureReport containing both MeasureDef and MeasureReport
     */
    @VisibleForTesting
    MeasureDefAndR4MeasureReport evaluateSingleMeasureCaptureDef(
            MeasureReference measure,
            MeasureEvaluationRequest request,
            IRepository resolvedRepo,
            Parameters parameters) {

        var params = request.parameters();
        measurePeriodValidator.validatePeriodStartAndEnd(params.periodStart(), params.periodEnd());

        var serviceUtils = new R4MeasureServiceUtils(resolvedRepo);
        if (measureEvaluationOptions.isEnsureSearchParameters()) {
            serviceUtils.ensureSupplementalDataElementSearchParameter();
        }

        var subjectRef = request.subject().id();
        var evalType = serviceUtils.getMeasureEvalType(params.reportType(), subjectRef);
        var subjects = expandSubjects(request.subject(), resolvedRepo);

        var r4Processor = new R4MeasureProcessor(resolvedRepo, this.measureEvaluationOptions);
        var measures = serviceUtils.getMeasures(List.of(measure));
        var context = Engines.forRepository(resolvedRepo, this.measureEvaluationOptions.getEvaluationSettings(), null);

        final CompositeEvaluationResultsPerMeasure compositeResults = r4Processor.evaluateMultiMeasuresWithCqlEngine(
                subjects, measures, params.periodStart(), params.periodEnd(), parameters, context);

        // The single-measure interface contract is one report: always group all subjects together.
        // evalType determines the report type (INDIVIDUAL / SUMMARY / SUBJECTLIST), not the grouping.
        final List<List<MeasureDefAndR4MeasureReport>> listOfList = populationOrSingleMeasureReport(
                r4Processor,
                serviceUtils,
                compositeResults,
                context,
                measures,
                subjects,
                params.periodStart(),
                params.periodEnd(),
                params.reportType(),
                evalType,
                params.reporter(),
                subjectRef);

        return listOfList.get(0).get(0);
    }

    @Override
    public Parameters evaluate(
            List<MeasureReference> measures, MeasureEvaluationRequest request, Parameters parameters) {
        return evaluateWithDefs(measures, request, repository, parameters).parameters();
    }

    /**
     * Test-visible evaluation method that captures both MeasureDef and MeasureReport for each measure.
     * <p>
     * <strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong>
     * </p>
     *
     * @param measures     list of MeasureReferences
     * @param request      evaluation request parameters
     * @param resolvedRepo fully configured repository (endpoints proxied, data federated)
     * @param parameters   CQL parameters
     * @return MeasureDefAndR4ParametersWithMeasureReports
     */
    @VisibleForTesting
    MeasureDefAndR4ParametersWithMeasureReports evaluateWithDefs(
            List<MeasureReference> measures,
            MeasureEvaluationRequest request,
            IRepository resolvedRepo,
            Parameters parameters) {

        var serviceUtils = new R4MeasureServiceUtils(resolvedRepo);
        var subjectRef = request.subject().id();
        var evalType = serviceUtils.getMeasureEvalType(request.parameters().reportType(), subjectRef);
        var subjects = expandSubjects(request.subject(), resolvedRepo);

        return toMeasureDefAndParametersResults(evaluateToListOfList(
                measures, evalType, subjects, subjectRef, request.parameters(), resolvedRepo, parameters));
    }

    private List<List<MeasureDefAndR4MeasureReport>> evaluateToListOfList(
            List<MeasureReference> measureRefs,
            MeasureEvalType evalType,
            List<String> subjects,
            @Nullable String subjectRef,
            MeasureEvaluationParameters params,
            IRepository resolvedRepo,
            Parameters cqlParameters) {

        measurePeriodValidator.validatePeriodStartAndEnd(params.periodStart(), params.periodEnd());

        var r4Processor = new R4MeasureProcessor(resolvedRepo, this.measureEvaluationOptions);
        var serviceUtils = new R4MeasureServiceUtils(resolvedRepo);

        if (measureEvaluationOptions.isEnsureSearchParameters()) {
            serviceUtils.ensureSupplementalDataElementSearchParameter();
        }

        final List<Measure> measures = serviceUtils.getMeasures(measureRefs);
        log.debug("evaluate-measure, measures to evaluate: {}", measures.size());

        var context = Engines.forRepository(resolvedRepo, this.measureEvaluationOptions.getEvaluationSettings(), null);

        final CompositeEvaluationResultsPerMeasure compositeResults = r4Processor.evaluateMultiMeasuresWithCqlEngine(
                subjects, measures, params.periodStart(), params.periodEnd(), cqlParameters, context);

        if (evalType.equals(MeasureEvalType.POPULATION) || evalType.equals(MeasureEvalType.SUBJECTLIST)) {
            return populationOrSingleMeasureReport(
                    r4Processor,
                    serviceUtils,
                    compositeResults,
                    context,
                    measures,
                    subjects,
                    params.periodStart(),
                    params.periodEnd(),
                    params.reportType(),
                    evalType,
                    params.reporter(),
                    subjectRef);
        }

        return subjectReport(
                r4Processor,
                serviceUtils,
                compositeResults,
                context,
                measures,
                subjects,
                params.periodStart(),
                params.periodEnd(),
                params.reportType(),
                evalType,
                params.reporter(),
                params.productLine());
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

    /**
     * Expands a subject reference to a concrete list of patient IDs.
     *
     * <p>Null {@code subject.id} returns all patients in the repository.
     * A Practitioner, Group, or Organization reference is resolved to their associated patients.
     * Always uses the resolved repository so endpoint/data configuration is honoured.
     */
    private List<String> expandSubjects(MeasureSubject subject, IRepository resolvedRepo) {
        String id = subject.id();
        return subjectProvider
                .getSubjects(resolvedRepo, id == null ? List.of() : List.of(id))
                .toList();
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

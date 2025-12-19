package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

@Deprecated
public class R4MeasureService implements R4MeasureEvaluatorSingle {
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasurePeriodValidator measurePeriodValidator;
    private final R4RepositorySubjectProvider subjectProvider;
    private final MeasureProcessorUtils measureProcessorUtils = new MeasureProcessorUtils();

    public R4MeasureService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.measurePeriodValidator = measurePeriodValidator;
        this.subjectProvider = new R4RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
    }

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

        return evaluateMeasureCaptureDefs(
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
     * @param measure Either canonical URL, ID, or Measure resource
     * @param periodStart start date of Measurement Period
     * @param periodEnd end date of Measurement Period
     * @param reportType type of report
     * @param subjectId the subject ID
     * @param lastReceivedOn last received on date
     * @param contentEndpoint content endpoint
     * @param terminologyEndpoint terminology endpoint
     * @param dataEndpoint data endpoint
     * @param additionalData additional data bundle
     * @param parameters CQL parameters
     * @param productLine product line
     * @param practitioner practitioner ID
     * @return MeasureDefAndR4MeasureReport containing both MeasureDef and MeasureReport
     */
    @VisibleForTesting
    MeasureDefAndR4MeasureReport evaluateMeasureCaptureDefs(
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

        measurePeriodValidator.validatePeriodStartAndEnd(periodStart, periodEnd);

        var proxyRepoForMeasureProcessor =
                Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);
        var processor = new R4MeasureProcessor(
                proxyRepoForMeasureProcessor, this.measureEvaluationOptions, measureProcessorUtils);

        R4MeasureServiceUtils r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        r4MeasureServiceUtils.ensureSupplementalDataElementSearchParameter();

        if (StringUtils.isNotBlank(practitioner)) {
            if (!practitioner.contains("/")) {
                practitioner = "Practitioner/".concat(practitioner);
            }
            subjectId = practitioner;
        }

        var evalType = r4MeasureServiceUtils.getMeasureEvalType(
                reportType, Optional.ofNullable(subjectId).map(List::of).orElse(List.of()));

        var subjects = getSubjects(subjectId, proxyRepoForMeasureProcessor, additionalData);

        // Replicate the old logic of using the repository used to initialize the measure processor
        // as the repository for the CQL engine context.
        var context = Engines.forRepository(
                proxyRepoForMeasureProcessor, this.measureEvaluationOptions.getEvaluationSettings(), additionalData);

        var evaluationResults =
                processor.evaluateMeasureWithCqlEngine(subjects, measure, periodStart, periodEnd, parameters, context);

        // Call processor's test-visible method to get both MeasureDef and MeasureReport
        MeasureDefAndR4MeasureReport result = processor.evaluateMeasureCaptureDef(
                measure, periodStart, periodEnd, reportType, subjects, evalType, context, evaluationResults);

        // add ProductLine after report is generated
        MeasureReport measureReport =
                r4MeasureServiceUtils.addProductLineExtension(result.measureReport(), productLine);

        // add subject reference for non-individual reportTypes
        measureReport = r4MeasureServiceUtils.addSubjectReference(measureReport, practitioner, subjectId);

        // Return new record with updated MeasureReport
        return new MeasureDefAndR4MeasureReport(result.measureDef(), measureReport);
    }

    @Nonnull
    private List<String> getSubjects(
            String subjectId, IRepository proxyRepoForMeasureProcessor, Bundle additionalData) {
        final IRepository repoToUseForSubjectProvider;
        if (additionalData != null) {
            repoToUseForSubjectProvider = new FederatedRepository(
                    this.repository, new InMemoryFhirRepository(this.repository.fhirContext(), additionalData));
        } else {
            repoToUseForSubjectProvider = proxyRepoForMeasureProcessor;
        }

        return subjectProvider
                .getSubjects(
                        repoToUseForSubjectProvider,
                        Optional.ofNullable(subjectId).map(List::of).orElse(List.of()))
                .toList();
    }
}

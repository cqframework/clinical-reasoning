package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
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

public class R4MeasureService implements R4MeasureEvaluatorSingle {
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasurePeriodValidator measurePeriodValidator;
    private final R4RepositorySubjectProvider subjectProvider;
    private final R4MeasureServiceUtils measureServiceUtils;
    private final MeasureProcessorUtils measureProcessorUtils = new MeasureProcessorUtils();

    public R4MeasureService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasurePeriodValidator measurePeriodValidator,
            R4MeasureServiceUtils measureServiceUtils) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.measurePeriodValidator = measurePeriodValidator;
        this.subjectProvider = new R4RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
        this.measureServiceUtils = measureServiceUtils;
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

        measurePeriodValidator.validatePeriodStartAndEnd(periodStart, periodEnd);

        var repo = Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);
        var processor = new R4MeasureProcessor(repo, this.measureEvaluationOptions, measureProcessorUtils);

        R4MeasureServiceUtils r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        r4MeasureServiceUtils.ensureSupplementalDataElementSearchParameter();

        var foldedMeasure = R4MeasureServiceUtils.foldMeasure(measure, repo);

        MeasureReport measureReport;

        if (StringUtils.isNotBlank(practitioner)) {
            if (!practitioner.contains("/")) {
                practitioner = "Practitioner/".concat(practitioner);
            }
            subjectId = practitioner;
        }

        var actualRepo = repo;
        if (additionalData != null) {
            actualRepo = new FederatedRepository(
                    this.repository, new InMemoryFhirRepository(this.repository.fhirContext(), additionalData));
        }

        var evalType = r4MeasureServiceUtils.getMeasureEvalType(
                reportType, Optional.ofNullable(subjectId).map(List::of).orElse(List.of()));

        var subjects = subjectProvider
                .getSubjects(
                        actualRepo, Optional.ofNullable(subjectId).map(List::of).orElse(List.of()))
                .toList();

        // LUKETODO:  which repository should we use for the context?
        var context = Engines.forRepository(
                this.repository, this.measureEvaluationOptions.getEvaluationSettings(), additionalData);

        var evaluationResults = processor.evaluateMeasureWithCqlEngine(
                subjects, foldedMeasure, periodStart, periodEnd, parameters, context, additionalData);

        measureReport = processor.evaluateMeasure(
                measure,
                periodStart,
                periodEnd,
                reportType,
                subjects,
                additionalData,
                parameters,
                evalType,
                context,
                evaluationResults);

        // add ProductLine after report is generated
        measureReport = r4MeasureServiceUtils.addProductLineExtension(measureReport, productLine);

        // add subject reference for non-individual reportTypes
        return r4MeasureServiceUtils.addSubjectReference(measureReport, practitioner, subjectId);
    }
}

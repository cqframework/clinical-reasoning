package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.Repositories;

public class R4MeasureService implements R4MeasureEvaluatorSingle {
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasurePeriodValidator measurePeriodValidator;
    private final R4RepositorySubjectProvider subjectProvider;
    private final R4MeasureServiceUtils measureServiceUtils;

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
        var processor = new R4MeasureProcessor(
                repo, this.measureEvaluationOptions, this.subjectProvider, this.measureServiceUtils);

        R4MeasureServiceUtils r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        r4MeasureServiceUtils.ensureSupplementalDataElementSearchParameter();

        MeasureReport measureReport;

        if (StringUtils.isNotBlank(practitioner)) {
            if (!practitioner.contains("/")) {
                practitioner = "Practitioner/".concat(practitioner);
            }
            subjectId = practitioner;
        }

        measureReport = processor.evaluateMeasure(
                measure,
                periodStart,
                periodEnd,
                reportType,
                Collections.singletonList(subjectId),
                additionalData,
                parameters);

        // add ProductLine after report is generated
        measureReport = r4MeasureServiceUtils.addProductLineExtension(measureReport, productLine);

        // add subject reference for non-individual reportTypes
        return r4MeasureServiceUtils.addSubjectReference(measureReport, practitioner, subjectId);
    }
}

package org.opencds.cqf.fhir.cr.measure.r4;

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
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.AdditionalDatas;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.Repositories;

public class R4MeasureService implements R4MeasureEvaluatorSingle {
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasurePeriodValidator measurePeriodValidator;
    private final R4MeasureServiceUtils measureServiceUtils;
    private final R4MeasureProcessor processor;

    public R4MeasureService(
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.measurePeriodValidator = measurePeriodValidator;
        this.measureServiceUtils = new R4MeasureServiceUtils(repository);
        this.processor =
                new R4MeasureProcessor(repository, this.measureEvaluationOptions, new R4RepositorySubjectProvider());
    }

    public MeasureReport evaluate(
            Either3<CanonicalType, IdType, Measure> measure,
            ZonedDateTime periodStart,
            ZonedDateTime periodEnd,
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
        Repository repo = this.repository;
        if (contentEndpoint != null || terminologyEndpoint != null || dataEndpoint != null) {
            repo = Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);
        }
        if (additionalData != null) {
            repo = AdditionalDatas.addAdditionalData(repo, additionalData);
        }
        var delegated = new R4MeasureService(repo, measureEvaluationOptions, measurePeriodValidator);
        return delegated.evaluate(
                measure,
                periodStart,
                periodEnd,
                reportType,
                subjectId,
                lastReceivedOn,
                parameters,
                productLine,
                practitioner);
    }

    @Override
    public MeasureReport evaluate(
            Either3<CanonicalType, IdType, Measure> measure,
            ZonedDateTime periodStart,
            ZonedDateTime periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
            Parameters parameters,
            String productLine,
            String practitioner) {

        measurePeriodValidator.validatePeriodStartAndEnd(periodStart, periodEnd);
        measureServiceUtils.ensureSupplementalDataElementSearchParameter();

        MeasureReport measureReport = null;

        if (StringUtils.isNotBlank(practitioner)) {
            if (!practitioner.contains("/")) {
                practitioner = "Practitioner/".concat(practitioner);
            }
            subjectId = practitioner;
        }

        measureReport = processor.evaluateMeasure(
                measure, periodStart, periodEnd, reportType, Collections.singletonList(subjectId), parameters);

        // add ProductLine after report is generated
        measureReport = measureServiceUtils.addProductLineExtension(measureReport, productLine);

        // add subject reference for non-individual reportTypes
        return measureServiceUtils.addSubjectReference(measureReport, practitioner, subjectId);
    }
}

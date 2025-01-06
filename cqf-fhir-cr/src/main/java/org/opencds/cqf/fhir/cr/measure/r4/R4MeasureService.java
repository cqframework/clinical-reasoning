package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.repository.Repositories;

public class R4MeasureService implements R4MeasureEvaluatorSingle {
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final MeasurePeriodValidator measurePeriodValidator;
    private final R4RepositorySubjectProvider subjectProvider;
    private final R4MeasureServiceUtils measureServiceUtils;

    public R4MeasureService(
            Repository repository,
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
    public MeasureReport evaluate(R4MeasureEvaluatorSingleRequest request) {

        measurePeriodValidator.validatePeriodStartAndEnd(request.getPeriodStart(), request.getPeriodEnd());

        var repo = Repositories.proxy(
                repository,
                true,
                request.getDataEndpoint(),
                request.getContentEndpoint(),
                request.getTerminologyEndpoint());
        var processor = new R4MeasureProcessor(
                repo, this.measureEvaluationOptions, this.subjectProvider, this.measureServiceUtils);

        R4MeasureServiceUtils r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        r4MeasureServiceUtils.ensureSupplementalDataElementSearchParameter();

        MeasureReport measureReport;

        var practitioner = request.getPractitioner();
        var subjectId = request.getSubjectId();

        if (StringUtils.isNotBlank(practitioner)) {
            if (!practitioner.contains("/")) {
                practitioner = "Practitioner/".concat(practitioner);
            }
            subjectId = practitioner;
        }

        measureReport = processor.evaluateMeasure(
                request.getMeasure(),
                request.getPeriodStart(),
                request.getPeriodEnd(),
                request.getReportType(),
                Collections.singletonList(subjectId),
                request.getAdditionalData(),
                request.getParameters());

        // add ProductLine after report is generated
        measureReport = r4MeasureServiceUtils.addProductLineExtension(measureReport, request.getProductLine());

        // add subject reference for non-individual reportTypes
        return r4MeasureServiceUtils.addSubjectReference(measureReport, practitioner, subjectId);
    }
}

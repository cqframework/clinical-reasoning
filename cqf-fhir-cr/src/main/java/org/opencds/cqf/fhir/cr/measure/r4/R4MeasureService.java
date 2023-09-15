package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.Collections;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.Repositories;

public class R4MeasureService {

    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;

    public R4MeasureService(Repository repository, MeasureEvaluationOptions measureEvaluationOptions) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
    }

    public MeasureReport evaluate(
            Either3<CanonicalType, IdType, Measure> measure,
            String periodStart,
            String periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData) {

        var repo = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
        var processor = new R4MeasureProcessor(repo, this.measureEvaluationOptions, new R4RepositorySubjectProvider());

        return processor.evaluateMeasure(
                measure, periodStart, periodEnd, reportType, Collections.singletonList(subjectId), additionalData);
    }
}

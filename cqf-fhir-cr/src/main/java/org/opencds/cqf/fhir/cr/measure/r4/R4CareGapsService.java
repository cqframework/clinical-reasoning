package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;

public class R4CareGapsService {

    private final Repository repository;

    private final MeasureEvaluationOptions measureEvaluationOptions;

    private CareGapsProperties careGapsProperties;

    private String serverBase;

    private final R4CareGapsProcessor r4CareGapsProcessor;

    public R4CareGapsService(
            CareGapsProperties careGapsProperties,
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase) {
        this.repository = repository;
        this.careGapsProperties = careGapsProperties;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.serverBase = serverBase;

        r4CareGapsProcessor =
                new R4CareGapsProcessor(careGapsProperties, repository, measureEvaluationOptions, serverBase);
    }

    /**
     * Calculate measures describing gaps in care
     *
     * @param periodStart
     * @param periodEnd
     * @param subject
     * @param statuses
     * @param measureIds
     * @param measureIdentifiers
     * @param measureUrls
     * @return Parameters that includes zero to many document bundles that include Care Gap Measure
     *         Reports will be returned.
     */
    public Parameters getCareGapsReport(
            IPrimitiveType<Date> periodStart,
            IPrimitiveType<Date> periodEnd,
            String subject,
            List<String> statuses,
            List<IdType> measureIds,
            List<String> measureIdentifiers,
            List<CanonicalType> measureUrls) {

        return r4CareGapsProcessor.getCareGapsReport(
                periodStart, periodEnd, subject, statuses, measureIds, measureIdentifiers, measureUrls);
    }
}

package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;

/**
 * Care Gap service that processes and produces care-gaps report as a result
 */
public class R4CareGapsService {
    private final R4CareGapsProcessor r4CareGapsProcessor;

    public R4CareGapsService(
            CareGapsProperties careGapsProperties,
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase,
            MeasurePeriodValidator measurePeriodEvalutator) {

        r4CareGapsProcessor = new R4CareGapsProcessor(
                careGapsProperties, repository, measureEvaluationOptions, serverBase, measurePeriodEvalutator);
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
     * @param isDocumentMode
     * @return Parameters that includes zero to many document bundles that include Care Gap Measure
     *         Reports will be returned.
     */
    public Parameters getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            @Nullable String subject,
            List<String> statuses,
            List<IdType> measureIds,
            List<String> measureIdentifiers,
            List<CanonicalType> measureUrls,
            @Nullable Boolean isDocumentMode) {

        return r4CareGapsProcessor.getCareGapsReport(
                periodStart, periodEnd, subject, statuses, measureIds, measureIdentifiers, measureUrls, isDocumentMode);
    }
}

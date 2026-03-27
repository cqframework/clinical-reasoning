package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;

/**
 * Care Gap service that processes and produces care-gaps report as a result
 */
public class R4CareGapsService implements R4CareGapsServiceInterface {
    private final R4CareGapsProcessor r4CareGapsProcessor;

    public R4CareGapsService(
            CareGapsProperties careGapsProperties,
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase,
            MeasurePeriodValidator measurePeriodEvalutator) {

        r4CareGapsProcessor = new R4CareGapsProcessor(
                careGapsProperties, repository, measureEvaluationOptions, serverBase, measurePeriodEvalutator);
    }

    /**
     * Calculate measures describing gaps in care
     *
     * @param periodStart measurement period starting interval.
     * @param periodEnd measurement period ending interval.
     * @param subject subject population to use for care-gap results. Accepted values [empty=all Patients, Patient/{id}, Group/{id} type {person, practitioner}, Practitioner/{id}]
     * @param status determines what care-gap statuses will be returned by the service if found. [prospective-gap, closed-gap, open-gap, not-applicable].
     *                 If Result is 'not-applicable' for Measure, but status requested was 'open-gap', then no result will be returned.
     * @param measureRefs Measures to check care-gap for, as a combined list of typed references (by ID, identifier, or canonical URL)
     * @param notDocument defaulted to 'false', if left empty. This parameter determines if standard care-gaps formatted 'document' bundle is returned with [composition, Detected Issue,
     *                       configured resources, evaluated resources, measure reports]. Or non-document mode,
     *                       which just returns a bundle with [Detected Issue(s)], with contained Measure Report.
     * @return Parameters that includes zero to many document bundles that include Care Gap Measure
     *         Reports will be returned.
     */
    @Override
    public Parameters getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            @Nullable String subject,
            List<String> status,
            List<MeasureReference> measureRefs,
            boolean notDocument) {

        return r4CareGapsProcessor.getCareGapsReport(periodStart, periodEnd, subject, status, measureRefs, notDocument);
    }
}

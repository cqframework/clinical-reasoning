package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;

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
     * @param periodStart measurement period starting interval.
     * @param periodEnd measurement period ending interval.
     * @param subject subject population to use for care-gap results. Accepted values [empty=all Patients, Patient/{id}, Group/{id} type {person, practitioner}, Practitioner/{id}]
     * @param status determines what care-gap statuses will be returned by the service if found. [prospective-gap, closed-gap, open-gap, not-applicable].
     *                 If Result is 'not-applicable' for Measure, but status requested was 'open-gap', then no result will be returned.
     * @param measureId Measures to check care-gap for by resolving by fhir resource id
     * @param measureIdentifier Measures to check care-gap for by resolving identifier value or systemUrl|value
     * @param measureUrl Measures to check care-gap for by resolving canonical url reference
     * @param notDocument defaulted to 'false', if left empty. This parameter determines if standard care-gaps formatted 'document' bundle is returned with [composition, Detected Issue,
     *                       configured resources, evaluated resources, measure reports]. Or non-document mode,
     *                       which just returns a bundle with [Detected Issue(s)], with contained Measure Report.
     * @return Parameters that includes zero to many document bundles that include Care Gap Measure
     *         Reports will be returned.
     */
    public Parameters getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            @Nullable String subject,
            List<String> status,
            List<IdType> measureId,
            List<String> measureIdentifier,
            List<CanonicalType> measureUrl,
            boolean notDocument) {

        return r4CareGapsProcessor.getCareGapsReport(
                periodStart,
                periodEnd,
                subject,
                status,
                liftMeasureParameters(measureId, measureIdentifier, measureUrl),
                notDocument);
    }

    protected List<Either3<IdType, String, CanonicalType>> liftMeasureParameters(
            List<IdType> measureId, List<String> measureIdentifier, List<CanonicalType> measureUrl) {

        List<Either3<IdType, String, CanonicalType>> eitherList = new ArrayList<>();

        measureId.stream()
                .filter(x -> x.getIdPart() != null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .forEach(id -> eitherList.add(Eithers.forLeft3(id)));
        measureIdentifier.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .forEach(identifier -> eitherList.add(Eithers.forMiddle3(identifier)));
        measureUrl.stream()
                .filter(Objects::nonNull)
                .filter(x -> !x.toString().contains("null"))
                .collect(Collectors.toList())
                .forEach(canonical -> eitherList.add(Eithers.forRight3(canonical)));
        if (eitherList.isEmpty()) {
            throw new IllegalArgumentException("no measure resolving parameter was specified");
        }
        return eitherList;
    }
}
